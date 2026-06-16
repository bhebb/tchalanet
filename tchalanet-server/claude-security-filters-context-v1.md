# Claude Implementation Note — Security Filters, SecurityConfig, and TchContextFilter

## Status

Draft for implementation

## Goal

Stabilize the provider-neutral authentication/authorization pipeline without relying on custom Spring Security filter ordering.

This note explains how to structure:

- `SecurityConfig`
- `SensitiveIdentityVerificationFilter`
- `TchAccessContextPipelineFilter`
- `TchContextFilter`
- `TenantContextResolver`
- `TchRequestContextFactory`

The target pipeline is:

```text
BearerTokenAuthenticationFilter
  -> validates Firebase/provider bearer token
  -> creates technical Spring Authentication

SensitiveIdentityVerificationFilter
  -> optional provider-level verification for sensitive endpoints

TchAccessContextPipelineFilter
  -> IdentityBootstrapStep
  -> AccessResolutionStep
  -> enrich Spring Authentication with ACTOR_*, ROLE_*, PERM_*

TchContextFilter
  -> build canonical TchRequestContext from ResolvedAccessContext
  -> hydrate tenant info
  -> bind MDC / ThreadLocal / RLS context
  -> resolve operational context

AuthorizationFilter
  -> method security / route authorization continues
```

## Core Decisions

### 1. Do not anchor custom filters on custom filters

Do not use:

```java
.addFilterAfter(tchContextFilter, TchAccessContextPipelineFilter.class)
```

Spring Security may fail with:

```text
The Filter class ... does not have a registered order
```

Custom filters must be anchored to known Spring Security filters.

Use:

```java
.addFilterAfter(tchAccessContextPipelineFilter, BearerTokenAuthenticationFilter.class)
.addFilterBefore(tchContextFilter, AuthorizationFilter.class)
```

### 2. `TchAccessContextPipelineFilter` does not bind context

`TchAccessContextPipelineFilter` should only run identity/access steps:

```text
IdentityBootstrapStep
AccessResolutionStep
Spring Authentication enrichment
```

It should not bind `TchRequestContext`, MDC, or RLS.

That responsibility belongs to `TchContextFilter`.

### 3. `TchContextFilter` must not resolve access

`TchContextFilter` must not decide roles, permissions, membership, or tenant access.

It consumes:

```text
ResolvedAccessContext
```

and builds/binds:

```text
TchRequestContext
```

### 4. Tenant access is resolved in `platform.accesscontrol`

The effective tenant must be resolved before `TchContextFilter`, in `AccessResolutionStep`, through an `EffectiveTenantResolver`.

`TchContextFilter` may hydrate tenant metadata, but it must not decide if the actor has access to a tenant.

### 5. SellerTerminal is actor type, not role

Use:

```text
ACTOR_SELLER_TERMINAL
PERM_terminal.sell
```

Do not use:

```text
ROLE_SELLER_TERMINAL
```

`TENANT_ADMIN`, `TENANT_OWNER`, and `SUPER_ADMIN` are roles.

`SELLER_TERMINAL` is an actor type.

---

# SecurityConfig Target

## SecurityConfig

```java
@Configuration
@Slf4j
public class SecurityConfig {

    @Value("${spring.websecurity.debug:false}")
    boolean webSecurityDebug;

    @Bean
    TchAccessContextPipelineFilter tchAccessContextPipelineFilter(
        IdentityBootstrapStep identityBootstrapStep,
        AccessResolutionStep accessResolutionStep
    ) {
        return new TchAccessContextPipelineFilter(
            identityBootstrapStep,
            accessResolutionStep
        );
    }

    @Bean
    SecurityFilterChain security(
        HttpSecurity http,
        JwtDecoder jwtDecoder,
        IdentityProviderApi identityProviderApi,
        TchAccessContextPipelineFilter tchAccessContextPipelineFilter,
        TchContextFilter tchContextFilter
    ) throws Exception {
        var sensitiveIdentityVerificationFilter =
            new SensitiveIdentityVerificationFilter(
                identityProviderApi,
                new SensitiveIdentityRequestMatcher()
            );

        http.csrf(AbstractHttpConfigurer::disable)
            .cors(withDefaults())
            .sessionManagement(sm -> sm.sessionCreationPolicy(STATELESS))
            .requestCache(RequestCacheConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD)
                .permitAll()

                .requestMatchers(
                    "/actuator/health",
                    "/actuator/health/**",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/openapi/**",
                    "/api/v1/openapi/**",
                    "/api/v1/swagger-ui/**",
                    "/api/v1/public/**",
                    "/api/v1/actuator/**",
                    "/public/**",
                    "/error",
                    "/api/v1/error"
                )
                .permitAll()

                // Optional infra/admin ops UI; review before production.
                .requestMatchers(
                    "/api/v1/admin/ops",
                    "/api/v1/admin/ops/**",
                    "/admin/ops",
                    "/admin/ops/**"
                )
                .permitAll()

                // Business authorization is enforced by @PreAuthorize.
                .anyRequest()
                .authenticated()
            )
            .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt
                .decoder(jwtDecoder)
                .jwtAuthenticationConverter(token -> convert(token, identityProviderApi))
            ))
            .addFilterAfter(
                sensitiveIdentityVerificationFilter,
                BearerTokenAuthenticationFilter.class
            )
            .addFilterAfter(
                tchAccessContextPipelineFilter,
                BearerTokenAuthenticationFilter.class
            )
            .addFilterBefore(
                tchContextFilter,
                AuthorizationFilter.class
            );

        return http.build();
    }

    private AbstractAuthenticationToken convert(
        Jwt jwt,
        IdentityProviderApi identityProviderApi
    ) {
        var externalUser =
            identityProviderApi.mapVerifiedToken(
                new VerifiedExternalToken(
                    jwt.getClaimAsString("iss"),
                    jwt.getSubject(),
                    jwt.getClaimAsString("email"),
                    Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified")),
                    jwt.getClaims()
                ),
                IdentityVerificationPolicy.STANDARD
            );

        log.debug("jwt.convert provider={} sub={}", externalUser.provider(), jwt.getSubject());

        // Authorities are empty here.
        // AccessResolutionStep will populate ACTOR_*, ROLE_*, PERM_* later.
        var authentication = new JwtAuthenticationToken(jwt, List.of());
        authentication.setDetails(externalUser);
        return authentication;
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.debug(webSecurityDebug);
    }

    /**
     * Prevent double servlet-container registration when TchContextFilter is also registered
     * explicitly in the Spring Security chain.
     */
    @Bean
    FilterRegistrationBean<TchContextFilter> tchContextFilterRegistration(
        TchContextFilter filter
    ) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
```

Required imports include:

```java
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
```

---

# TchAccessContextPipelineFilter

## Responsibility

This filter runs after bearer token authentication.

It is responsible for:

```text
1. IdentityBootstrapStep.bootstrap(request)
2. AccessResolutionStep.resolve(request)
```

`AccessResolutionStep` must also enrich Spring Authentication with:

```text
ACTOR_*
ROLE_*
PERM_*
```

This filter must not bind `TchRequestContext`.

## Implementation

```java
package com.tchalanet.server.app.config.security;

import com.tchalanet.server.platform.accesscontrol.api.AccessResolutionStep;
import com.tchalanet.server.platform.identity.api.IdentityBootstrapStep;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

public final class TchAccessContextPipelineFilter extends OncePerRequestFilter {

    private final IdentityBootstrapStep identityBootstrapStep;
    private final AccessResolutionStep accessResolutionStep;

    public TchAccessContextPipelineFilter(
        IdentityBootstrapStep identityBootstrapStep,
        AccessResolutionStep accessResolutionStep
    ) {
        this.identityBootstrapStep = identityBootstrapStep;
        this.accessResolutionStep = accessResolutionStep;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        return path.startsWith("/public/")
            || path.startsWith("/api/v1/public/")
            || path.equals("/actuator/health")
            || path.startsWith("/actuator/health/")
            || path.startsWith("/api/v1/actuator/health")
            || path.equals("/swagger-ui.html")
            || path.startsWith("/swagger-ui/")
            || path.startsWith("/api/v1/swagger-ui/")
            || path.startsWith("/v3/api-docs/")
            || path.startsWith("/openapi/")
            || path.startsWith("/api/v1/openapi/")
            || path.equals("/error")
            || path.equals("/api/v1/error");
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        identityBootstrapStep.bootstrap(request);
        accessResolutionStep.resolve(request);
        filterChain.doFilter(request, response);
    }
}
```

---

# SensitiveIdentityVerificationFilter

## Responsibility

This remains provider-token-level only.

It may check:

```text
issuer
email verified
token freshness
provider policy
sensitive policy
```

It must not decide:

```text
tenant
roles
permissions
SellerTerminal status
business access
```

If terminal technical users are introduced, the sensitive policy must avoid assuming all actors have verified human emails.

Possible future split:

```text
ProviderSensitiveIdentityVerificationFilter
ActorSensitiveVerificationFilter
```

---

# AccessResolutionStep

## Responsibility

`AccessResolutionStep` consumes:

```text
BootstrappedActor
```

and produces:

```text
ResolvedAccessContext
```

It must also enrich Spring Authentication.

## What it decides

For `APP_USER`:

```text
effective tenant
super admin status
tenant override
role codes
permission keys
```

For `SELLER_TERMINAL`:

```text
effective tenant from BootstrappedActor
actor authority ACTOR_SELLER_TERMINAL
terminal permissions if allowed by identity mapping
```

The actual business status check `seller_terminal.status == ACTIVE` must still be done by the sale handler in `seller-terminal-v0`.

## Do not resolve tenant by only parsing headers

Do not leave tenant resolution as:

```java
var tenantIdHeader = request.getHeader(TchHeaders.X_TENANT_ID);
effectiveTenantId = TenantId.of(UUID.fromString(tenantIdHeader));
```

That only reads a selector. It does not validate access.

Use an `EffectiveTenantResolver`.

## EffectiveTenantResolver

Target behavior:

```text
TENANT/ADMIN normal user:
  - if X-Tenant-Id is provided, validate active membership
  - else find exactly one active membership in V1
  - require tenant active

PLATFORM super admin:
  - no effective tenant unless explicit override

SUPER_ADMIN tenant override:
  - require explicit override header or selector according to policy
  - require platform tenant override permission
  - require active target tenant
  - set tenantOverride = true

SELLER_TERMINAL:
  - tenant comes from BootstrappedActor
  - terminal does not choose tenant via headers
```

---

# TchContextFilter

## Responsibility

`TchContextFilter` is the canonical context binder.

It must:

```text
read ResolvedAccessContext
build TchRequestContext
hydrate tenant metadata
bind context to request/thread/MDC/RLS
resolve operational context
clear context in finally
```

It must not:

```text
read JWT claims
resolve AppUser
load roles
load permissions
decide tenant membership
decide tenant override permission
```

## Required Flow Split

The filter must split early:

```java
var resolvedAccess = (ResolvedAccessContext)
    req.getAttribute(TchContextRequestAttributes.RESOLVED_ACCESS);

if (resolvedAccess != null) {
    handleResolvedAccess(req, res, chain, resolvedAccess);
    return;
}

handlePublicOrLegacy(req, res, chain);
```

## Provider-neutral protected flow

```java
private void handleResolvedAccess(
    HttpServletRequest req,
    HttpServletResponse res,
    FilterChain chain,
    ResolvedAccessContext resolvedAccess
) throws ServletException, IOException {

    var scope = ApiScopeResolver.resolve(req);

    if ((scope == ApiScope.TENANT || scope == ApiScope.ADMIN)
        && resolvedAccess.effectiveTenantId() == null) {
        res.sendError(HttpServletResponse.SC_FORBIDDEN, "Tenant context required");
        return;
    }

    if (resolvedAccess.tenantOverride()
        && StringUtils.isBlank(req.getHeader(X_TCH_OVERRIDE_REASON))) {
        res.sendError(HttpServletResponse.SC_FORBIDDEN, "Super-admin override reason required");
        return;
    }

    var ctx = contextFactory.createFromResolvedAccess(req, scope, resolvedAccess);

    // Hydrate tenant metadata only. Do not decide tenant access here.
    ctx = tenantContextResolver.hydrateResolvedTenant(res, ctx);

    if (ctx == null) {
        return;
    }

    contextBinder.bind(req, ctx);

    var resolver = operationalContextResolver.getIfAvailable();

    ctx = ctx.withOperationalContext(
        resolver == null
            ? OperationalContextHeaderParser.parseHint(req::getHeader)
            : resolver.resolve(ctx, req::getHeader)
    );

    contextBinder.bind(req, ctx);

    chain.doFilter(req, res);
}
```

## Public / legacy flow

```java
private void handlePublicOrLegacy(
    HttpServletRequest req,
    HttpServletResponse res,
    FilterChain chain
) throws ServletException, IOException {

    var scope = ApiScopeResolver.resolve(req);

    var defaultTenantCode =
        ApiScopeResolver.allowDefaultTenant(req)
            ? normalize(contextProperties.publicDefaultTenantCode())
            : null;

    var ctx = contextFactory.createPublic(req, defaultTenantCode, scope);

    ctx = tenantContextResolver.resolveForScope(req, res, ctx, scope, defaultTenantCode);

    if (ctx == null) {
        return;
    }

    contextBinder.bind(req, ctx);

    var resolver = operationalContextResolver.getIfAvailable();

    ctx = ctx.withOperationalContext(
        resolver == null
            ? OperationalContextHeaderParser.parseHint(req::getHeader)
            : resolver.resolve(ctx, req::getHeader)
    );

    contextBinder.bind(req, ctx);

    chain.doFilter(req, res);
}
```

## Full skeleton

```java
@Override
protected void doFilterInternal(
    @Nonnull HttpServletRequest req,
    @Nonnull HttpServletResponse res,
    @Nonnull FilterChain chain
) throws ServletException, IOException {

    try {
        var resolvedAccess = (ResolvedAccessContext)
            req.getAttribute(TchContextRequestAttributes.RESOLVED_ACCESS);

        if (resolvedAccess != null) {
            handleResolvedAccess(req, res, chain, resolvedAccess);
            return;
        }

        handlePublicOrLegacy(req, res, chain);

    } finally {
        contextBinder.clear(req);
    }
}
```

---

# TenantContextResolver

## New method required

Keep existing `resolveForScope(...)` for public/legacy.

Add a method for provider-neutral protected requests:

```java
public TchRequestContext hydrateResolvedTenant(
    HttpServletResponse res,
    TchRequestContext ctx
) throws IOException {
    if (ctx.tenantIdSafe() == null) {
        return ctx;
    }

    var info = tenantLookup.findById(ctx.tenantIdSafe());

    if (info.isEmpty()) {
        res.sendError(HttpServletResponse.SC_FORBIDDEN, "Tenant not found");
        return null;
    }

    return ctx.withTenantContext(info.get());
}
```

This method only hydrates:

```text
tenant code
tenant UUID
timezone
currency
```

It must not validate membership or permissions.

---

# TchRequestContextFactory

## Remove AuthContextExtractor

Remove:

```java
private final AuthContextExtractor authContextExtractor;
```

The new protected path uses:

```java
createFromResolvedAccess(HttpServletRequest req, ApiScope scope, ResolvedAccessContext resolved)
```

The public/legacy path uses:

```java
createPublic(HttpServletRequest req, String defaultTenantCode, ApiScope scope)
```

Keep:

```java
create(HttpServletRequest req, String defaultTenantCode, ApiScope scope)
```

only as a compatibility alias to `createPublic(...)`.

## Important

Do not call:

```java
create(req, resolved.effectiveTenantId(), scope)
```

because the legacy `create(...)` expects a tenant code string, not a typed tenant ID.

---

# Module Boundaries

## Correct placement

```text
tchalanet-app
  SecurityConfig
  TchAccessContextPipelineFilter
  SensitiveIdentityVerificationFilter

platform.identity
  IdentityBootstrapStep
  UserBootstrapFilterImpl / Bootstrap implementation

platform.accesscontrol
  AccessResolutionStep
  EffectiveTenantResolver
  permission / role / membership resolution

common.context.web
  TchContextFilter
  TchRequestContextFactory

common.context.tenant
  TenantContextResolver
  TenantContextLookup

platform.tenant
  TenantPreContextLookupApi
  TenantContextLookup adapter implementation
```

## Forbidden direction

`common.context` must not depend on:

```text
platform.identity
platform.accesscontrol
platform.tenant
core.terminal
```

Use ports/adapters.

---

# Acceptance Criteria

## Security chain

- `/public/**` works without token.
- `/api/v1/public/**` works without token.
- `/tenant/**` without token returns `401`.
- `/admin/**` without token returns `401`.
- `/platform/**` without token returns `401`.
- No custom filter is used as an anchor in `addFilterAfter` / `addFilterBefore`.
- No `registered order` exception occurs at startup.

## Provider-neutral access

- Provider JWT roles/claims do not become business authorities.
- AppUser access is resolved from Tchalanet DB.
- Spring Authentication receives:
  - `ACTOR_APP_USER`
  - `ROLE_*`
  - `PERM_*`
- SellerTerminal support is represented by:
  - `ACTOR_SELLER_TERMINAL`
  - `PERM_terminal.*`

## Tenant context

- For protected requests, `effectiveTenantId` comes from `ResolvedAccessContext`.
- `TchContextFilter` does not re-resolve tenant from headers for protected requests.
- `TenantContextResolver.hydrateResolvedTenant(...)` only hydrates tenant metadata.
- RLS receives the tenant from `TchRequestContext`.

## Cleanup

- `AuthContextExtractor` is no longer required by `TchRequestContextFactory`.
- `TchContextFilter` is not registered twice.
- `TchContextFilter` clears context in `finally`.
