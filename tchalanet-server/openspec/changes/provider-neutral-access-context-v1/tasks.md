# Tasks: Provider-Neutral Access Context V1

## 0. Preparation

See `inventory.md` for full findings. Summary:

- [x] Inventory existing identity/access migrations. (V100–V230 read; see inventory.md)
- [x] List tables already present. (see inventory.md §1)
- [x] Confirm names already created by `access-control-v1`. (V202 — no conflicts)
- [x] Decide whether to reuse existing tables or rename through migration.
  - `role_permission` (not `app_role_permission`) — reuse, no rename
  - `user_permission_override` (not `tenant_user_permission_override`) — reuse, no rename
  - `platform_user_role` does not exist; resolve platform roles from `tenant_user_role JOIN app_role WHERE scope = 'PLATFORM'`
- [x] Document final table names in the OpenSpec. (inventory.md §1)
- [x] Add the V0 pivot note:
  - field sellers are no longer new AppUsers with `CASHIER` role;
  - field sellers are represented by `SellerTerminal`;
  - Firebase may authenticate SellerTerminal technical users;
  - Tchalanet authorizes terminal sales through SellerTerminal state.

## 1. SecurityConfig — route-level simplification

### 1.1 Modify SecurityConfig

- [x] `/public/**` -> `permitAll`.
- [x] `/actuator/health/**` -> `permitAll`.
- [x] everything else -> `authenticated`.
- [x] Remove business rules from global SecurityConfig:
  - no `hasRole("TENANT_ADMIN")`;
  - no `hasRole("SUPER_ADMIN")`;
  - no `hasAuthority("ticket.sell")`;
  - no terminal business decisions.

Expected shape:

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/public/**").permitAll()
    .requestMatchers("/actuator/health/**").permitAll()
    .anyRequest().authenticated()
)
```

### 1.2 Tests

- [x] `/public/**` works without token.
- [x] `/actuator/health/**` works without token.
- [x] `/tenant/**` without token returns `401`.
- [x] `/admin/**` without token returns `401`.
- [x] `/platform/**` without token returns `401`.

## 2. Database — validation and migrations

### 2.1 Inventory tables

See `inventory.md` for full details. Summary status:

- [x] Verify `app_user`. EXISTS (V100)
- [x] Verify `app_user_external_identity`. EXISTS (V100)
- [x] Verify `tenant_user`. EXISTS (V100) — has RLS, must be removed
- [x] Verify `tenant_user_role`. EXISTS (V100) — no RLS
- [x] Verify `platform_user_role`. NOT EXISTS — resolve platform roles from `tenant_user_role JOIN app_role WHERE scope = 'PLATFORM'`; no separate table needed
- [x] Verify `app_role`. EXISTS (V100, name confirmed) — no RLS
- [x] Verify `permission`. EXISTS (V100) — no RLS
- [x] Verify `app_role_permission`. EXISTS as `role_permission` (V100) — no RLS; reuse, no rename
- [x] Verify `tenant_user_permission_override`. EXISTS as `user_permission_override` (V100) — no RLS; reuse, no rename
- [x] Verify existing terminal/user/seller/cashier tables. `terminal`, `seller`, `seller_outlet_assignment`, `seller_commission_policy` all exist under RLS
- [x] Verify terminal identity mapping table. None exists; deferred to `seller-terminal-v0`

### 2.2 Final expected bootstrap/access tables

If missing, create or adjust:

```text
app_user
app_user_external_identity
tenant_user
tenant_user_role
platform_user_role
app_role
permission
app_role_permission
tenant_user_permission_override
```

Note: `seller_terminal` and `seller_terminal_external_identity` are deferred to `seller-terminal-v0`
(see §2.3).

### 2.3 SellerTerminal tables — deferred

`seller_terminal` and `seller_terminal_external_identity` are deferred entirely to
`seller-terminal-v0`. Do not create either table in this change.

SellerTerminal pipeline logic (IdentityBootstrapFilter branch, AccessResolutionFilter terminal
flow) is implemented and covered by unit tests with stubs. Integration tests against a real
`SELLER_TERMINAL` identity flow depend on `seller-terminal-v0` landing first.

### 2.4 Constraints

`app_user_external_identity`:

```text
unique(provider, issuer, external_subject)
```

`tenant_user`:

```text
unique active membership per app_user_id for V1
```

Example:

```sql
CREATE UNIQUE INDEX uq_tenant_user_one_active_per_app_user
ON tenant_user(app_user_id)
WHERE status = 'ACTIVE';
```

`tenant_user_role`:

```text
unique(tenant_user_id, role_code)
```

`platform_user_role`:

```text
unique(app_user_id, role_code)
```

`app_role_permission`:

```text
unique(role_code, permission_key)
```

`tenant_user_permission_override`:

```text
unique(tenant_user_id, permission_key)
```

`seller_terminal_external_identity`:

```text
unique(provider, issuer, external_subject)
unique(seller_terminal_id, provider)
```

## 3. RLS — reviewed bootstrap exceptions

### 3.1 Classify tables

Document classification.

Bootstrap identity/access tables — reviewed non-RLS exception candidate:

```text
app_user_external_identity
app_user minimal status lookup
tenant_user
tenant_user_role
platform_user_role
app_role
permission
app_role_permission
tenant_user_permission_override
```

Note: `seller_terminal_external_identity` is deferred to `seller-terminal-v0` and excluded from
the RLS exception classification in this change.

Business tenant-scoped tables — RLS mandatory:

```text
seller_terminal
ticket
ticket_line
payout
terminal legacy table if retained
outlet
draw
draw_result
odds_profile
odds_rule
limit_profile
limit_rule
audit_log tenant-scoped
```

### 3.2 If RLS policies exist on bootstrap tables

- [x] List existing policies. (V105: `tenant_user_rls_all`, `tenant_user_rls_select`)
- [x] Remove RLS policies only from approved bootstrap tables. (V231: tenant_user only)
- [x] Do not remove RLS from business tenant-scoped tables. (V231 is scoped to tenant_user)
- [x] Document the reason:
  - bootstrap tables are required to resolve actor, AppUser, terminal, tenant, roles, and permissions before context exists;
  - they are protected by module boundaries, repositories, constraints, status checks, and audit.

### 3.3 Migration

- [x] Create Flyway migration only after table classification is approved. (`V231__access_context_v1_schema.sql`)
- [x] Do not include `seller_terminal` business profile in the bootstrap exception unless explicitly approved.

## 4. Seeds — roles and permissions

> **Slice 2 COMPLETE** — V231 (schema) and V232 (seeds) created. V231 removes tenant_user RLS
> and adds partial unique index. V232 adds TENANT_OWNER (UUID …000305), 19 missing permissions,
> and role-permission matrix for TENANT_OWNER/TENANT_ADMIN/SUPER_ADMIN.

### 4.1 Role seeds

Create or adjust target V0 roles:

```text
TENANT_OWNER   scope TENANT
TENANT_ADMIN   scope TENANT
SUPER_ADMIN    scope PLATFORM
```

Legacy compatibility:

```text
CASHIER        legacy only, not used for new SellerTerminal flows
SELLER         legacy only if already present
```

### 4.2 Permission seeds

Admin/platform permissions:

```text
terminal.read
terminal.manage
terminal.block
terminal.reset_pin

sales.read
sales.report.read

ticket.read
ticket.void

odds.read
odds.manage

limit.read
limit.manage

draw_result.read
draw_result.manage
draw_result.confirm

payout.read
payout.mark_paid

report.read
billing.read

platform.access
platform.tenant.manage
platform.ops.run
```

Terminal-derived permissions:

```text
terminal.me.read
terminal.sell
terminal.ticket.read_own
terminal.ticket.reprint_own
```

Terminal-derived permissions may be produced by access resolution rather than seeded in role mappings.

### 4.3 Role-permission mapping

`TENANT_OWNER`:

```text
all tenant permissions
```

`TENANT_ADMIN` minimal V0:

```text
terminal.read
terminal.manage
terminal.block
terminal.reset_pin
sales.read
sales.report.read
ticket.read
ticket.void
odds.read
odds.manage
limit.read
limit.manage
draw_result.read
draw_result.manage
draw_result.confirm
payout.read
payout.mark_paid
report.read
billing.read
```

`SUPER_ADMIN`:

```text
platform.access
platform.tenant.manage
platform.ops.run
```

### 4.4 Super admin tenant override

Decision:

```text
without override: platform permissions only
with valid tenant override: ROLE_SUPER_ADMIN + synthetic tenant override authority set equivalent to TENANT_ADMIN for V1
```

## 5. Runtime contracts

> **Slice 1 COMPLETE** — all items below are implemented and tested (68 tests green in tchalanet-common).

### 5.1 External authenticated principal

In `platform.identity.api`, create or confirm:

```java
public record ExternalAuthenticatedUser(
    String provider,
    String issuer,
    String externalSubject,
    String email,
    String displayName,
    boolean emailVerified
) {}
```

### 5.2 Actor type [x DONE]

In `common.context` or `common.security`, create:

```java
public enum TchActorType {
    APP_USER,
    SELLER_TERMINAL,
    SYSTEM
}
```

### 5.3 BootstrappedActor [x DONE]

In `common.context`, create:

```java
public record BootstrappedActor(
    TchActorType actorType,
    UserId appUserId,
    SellerTerminalId sellerTerminalId,
    TenantId tenantId,
    String provider,
    String issuer,
    String externalSubject
) {}
```

### 5.4 UserAccessSnapshot

In `platform.accesscontrol.api`, create/confirm:

```java
public record UserAccessSnapshot(
    UserId appUserId,
    TenantId effectiveTenantId,
    boolean superAdmin,
    boolean tenantOverride,
    Set<String> roleCodes,
    Set<String> permissionKeys
) {}
```

### 5.5 ResolvedAccessContext [x DONE]

In `common.context`, create:

```java
public record ResolvedAccessContext(
    TchActorType actorType,
    UserId appUserId,
    SellerTerminalId sellerTerminalId,
    TenantId effectiveTenantId,
    boolean superAdmin,
    boolean tenantOverride,
    Set<String> roleCodes,
    Set<String> permissionKeys
) {}
```

Important:

```text
ResolvedAccessContext is temporary.
After TchContextFilter, the only canonical runtime context is TchRequestContext.
```

### 5.6 Request attributes [x DONE]

Create constants:

```java
BOOTSTRAPPED_ACTOR
RESOLVED_ACCESS
TENANT_OVERRIDE
```

## 6. IdentityBootstrapFilter

> **Slice 3 COMPLETE** — `UserBootstrapFilterImpl` extended; `SellerTerminalExternalIdentityPort`
> created (stub impl always returns empty until seller-terminal-v0). 201 tests pass (pre-existing
> 14 failures unchanged).
>
> **SensitiveIdentityVerificationFilter audit:** provider-token only — reads JWT claims,
> calls `identityProviderApi.mapVerifiedToken`, sets `ExternalAuthenticatedUser` as auth details.
> No AppUser/tenant/permissions touched. No changes needed; works for SellerTerminal JWTs.

### 6.1 Responsibilities

- [x] Read technical external principal already authenticated by provider verification.
- [x] Resolve `(provider, issuer, externalSubject)`.
- [x] Try AppUser external identity.
- [x] If found: load AppUser; verify status `ACTIVE`; attach `BootstrappedActor(APP_USER, ...)`.
- [x] Else try SellerTerminal external identity.
- [x] If found: verify identity record status; attach `BootstrappedActor(SELLER_TERMINAL, ...)`.
- [x] Else deny according to accepted identity policy.

### 6.2 Non-responsibilities

The filter must not:

- resolve AppUser tenant access;
- load AppUser roles;
- load AppUser permissions;
- bind RLS;
- construct `TchRequestContext`;
- execute sale terminal business validation.

### 6.3 Errors

- [x] Invalid provider identity: `401`.
- [x] Unlinked external identity: `403` (`external_identity.not_linked`).
- [x] AppUser disabled/locked: `403`.
- [x] SellerTerminal identity disabled/invalid: `403` (`terminal.not_active`).

### 6.4 Tests

- [x] active AppUser passes — sets `BOOTSTRAPPED_APP_USER_ID` + `BOOTSTRAPPED_ACTOR(APP_USER)`.
- [x] disabled AppUser is blocked before controller.
- [x] unknown external identity is blocked with stable error code.
- [x] provider mismatch is blocked (all IdentityProviderType variants parameterized).
- [x] SellerTerminal external identity resolves actor type `SELLER_TERMINAL`.
- [x] SellerTerminal blocked/disabled identity is blocked.

## 7. AccessResolutionFilter

### 7.1 Responsibilities — Slice 4 COMPLETE

- [x] Read `BOOTSTRAPPED_ACTOR`.
- [x] Resolve HTTP scope:
  - `PUBLIC`
  - `TENANT`
  - `ADMIN`
  - `PLATFORM`
- [x] For `APP_USER`, resolve via `EffectivePermissionService` + `TenantUserRoleJpaRepository` + `AppRoleJpaRepository`.
- [x] For `SELLER_TERMINAL`, resolve terminal effective tenant and derived terminal permissions.
- [x] Validate `SUPER_ADMIN` tenant override when requested.
- [x] Attach `RESOLVED_ACCESS`.
- [x] Enrich Spring Authentication with:
  - `ROLE_*`
  - `PERM_*`
  - `ACTOR_*`.

### 7.2 AppUser tenant flow

- [x] Load `tenant_user_role` via `EffectivePermissionService.getEffectivePermissions()`.
- [x] Load permissions via `app_role_permission` (DENY already applied by service).
- [x] Apply `tenant_user_permission_override` (handled by `EffectivePermissionService`).
- [x] Remove permissions explicitly `DENY` (service returns net-effective set).
- [x] Return tenant + roles + permissions.

### 7.3 Super admin flow

- [x] Load `platform_user_role` via `TenantUserRoleJpaRepository.findActivePlatformRoleIdsByUser()`.
- [x] `/platform/**` without override: effective tenant = null; role = `SUPER_ADMIN`.
- [x] with override (`X-Tch-Tenant-Override` header): effective tenant = target tenant; roles = `SUPER_ADMIN` + tenant override set.
- [x] verify tenant active (handled by `TenantContextResolver.resolveForScope` in `TchContextFilter`).
- [x] audit override (handled by `TchContextFilter` log.info on `tenant_override.active`).

### 7.4 SellerTerminal flow

- [x] Effective tenant resolved from `BootstrappedActor.tenantId()` (set by `IdentityBootstrapFilter`).
- [x] Attach `ACTOR_SELLER_TERMINAL` to Spring Authentication.
- [x] Attach pre-context terminal permissions (optimistic grants):
  - `PERM_terminal.me.read`
  - `PERM_terminal.sell`
  - `PERM_terminal.ticket.read_own`
  - `PERM_terminal.ticket.reprint_own`

### 7.5 Spring authorities

- [x] `TENANT_OWNER` → `ROLE_TENANT_OWNER`
- [x] `TENANT_ADMIN` → `ROLE_TENANT_ADMIN`
- [x] `SUPER_ADMIN` → `ROLE_SUPER_ADMIN`
- [x] `APP_USER` → `ACTOR_APP_USER`
- [x] `SELLER_TERMINAL` → `ACTOR_SELLER_TERMINAL`
- [x] `ticket.void` → `PERM_ticket.void`
- [x] `terminal.sell` → `PERM_terminal.sell`

### 7.6 Tests — 9/9 passing

- [x] tenant admin receives `ROLE_TENANT_ADMIN`.
- [x] tenant owner receives `ROLE_TENANT_OWNER`.
- [x] permission DENY removes authority.
- [x] super admin platform receives `ROLE_SUPER_ADMIN` without tenant.
- [x] super admin override receives effective tenant + expected roles.
- [x] user without active membership is refused on `/tenant/**` and `/admin/**`.
- [x] seller terminal receives `ACTOR_SELLER_TERMINAL`.
- [x] active seller terminal receives `PERM_terminal.sell`.
- [x] blocked seller terminal does not receive `PERM_terminal.sell` (N/A — terminal status is enforced by handler, not filter; IdentityBootstrapFilter blocks BLOCKED terminals before this point).

## 8. TchContextFilter / canonical context binder

### 8.1 Responsibilities

- [x] Read `RESOLVED_ACCESS`.
- [x] Add request id / trace id.
- [x] Add locale / timezone.
- [x] Build actor-neutral `TchRequestContext`:
  - `actorType`;
  - `appUserId` nullable;
  - `sellerTerminalId` nullable;
  - `effectiveTenantId` nullable for platform;
  - roles;
  - permissions.
- [x] Resolve operational context if present:
  - terminal;
  - outlet;
  - sales session.
- [x] For `SELLER_TERMINAL`, set operational terminal context from `sellerTerminalId`.
- [x] Bind: request attribute; ThreadLocal; MDC; RLS datasource facts.
- [x] Clear in `finally`.

### 8.2 Rules — Slice 5 COMPLETE

- [x] `TchContextFilter` does not import `platform.identity`.
- [x] `TchContextFilter` does not import `platform.accesscontrol`.
- [x] `TchContextFilter` does not import `core.terminal`.
- [x] `TchContextFilter` consumes only neutral `common.context` contracts.
- [x] `actorAuthorizationContextResolver` removed from TchContextFilter — DB resolution is now done exclusively by `AccessResolutionFilter`.
- [x] `X-Tenant-Id` removed from `hasSensitiveOverrideHeaders` — it is the standard provider-neutral tenant selector, not a SUPER_ADMIN-only header; `X-Tch-Tenant-Override` and `X-Deleted-Visibility` still require SUPER_ADMIN.
- [x] `TenantContextResolver.requireAndResolveTenant` gains fast path for pre-injected `tenantIdSafe()` (supports Firebase/provider-neutral tokens without JWT tenant claim).
- [x] `AccessResolutionFilter` added to `SecurityFilterRegistrationConfig` to prevent double-fire.

### 8.3 Tests — 7/7 passing

- [x] context cleared after request.
- [x] tenant/admin without effective tenant blocks before controller.
- [x] platform super admin without override can have tenant null.
- [x] SellerTerminal context has actorType and terminal id.
- [x] RLS receives expected tenant.
- [x] resolved access sets roles, permissions, appUserId.
- [x] sensitive-only header (X-Deleted-Visibility) without super admin is blocked.

## 9. TchPermissionEvaluator

### 9.1 Implementation V1

- [x] Read `TchRequestContext`.
- [x] Normalize requested permission.
- [x] Return `false` if context absent.
- [x] Return `false` if permission absent.
- [x] Return `true` if `ctx.permissions()` contains requested permission.
- [x] Do not call repositories directly.
- [x] Do not read JWT/provider token.

### 9.2 Tests

- [x] permission present returns true.
- [x] permission absent returns false.
- [x] context absent returns false.
- [x] permission DENY returns false through resolved permissions.
- [x] terminal permission `terminal.sell` works for active terminal.

## 10. Batch and scheduler context

### 10.1 Batch context binder

- [x] Create or adjust `BatchTchContextBinder`.
- [x] Support actor type `SYSTEM`.
- [x] Support context `TENANT`.
- [x] Support context `PLATFORM`.
- [x] Bind before DB access.
- [x] Clear in `finally`.

### 10.2 Rules

- [x] Batch does not pass through `IdentityBootstrapFilter`.
- [x] Batch does not pass through `AccessResolutionFilter`.
- [x] Batch does not pass through HTTP filters.
- [x] Batch uses actor `SYSTEM`.
- [x] Batch uses explicit system authorities allowlisted by job family.

### 10.3 Tests

- [x] batch tenant has effective tenant.
- [x] batch platform has platform scope.
- [x] batch context is cleared after execution.
- [x] no context leak between jobs.

## 11. Controllers — migration by surfaces

### 11.1 Inventory

- [x] List controllers under `/tenant/**`.
- [x] List controllers under `/admin/**`.
- [x] List controllers under `/platform/**`.
- [x] List terminal seller controllers/surfaces.
- [x] List sensitive actions:
  - sell → `hasPermission('terminal.sell')`;
  - void → `hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN')`;
  - payout mark paid → admin;
  - result create/confirm/correct → admin;
  - terminal create/block/unblock/disable/reset access → admin;
  - odds manage → admin;
  - limit manage → admin;
  - tenant override → SUPER_ADMIN;
  - ops force actions → SUPER_ADMIN.

### 11.2 Role checks on admin surfaces

- [x] All `/admin/**` controllers: `hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN')`.
- [x] TENANT_OWNER added everywhere it was missing.

### 11.3 Actor checks on terminal surfaces

- [x] `CurrentOperationalContextController`, `TerminalTenantRuntimeController`: `hasAuthority('ACTOR_SELLER_TERMINAL')`.
- [x] `TicketSalesController`, `SalePreparationController`: `hasPermission('terminal.sell')`.
- [x] Mixed surfaces (ticket query, sessions, payouts, outlet): `hasAnyRole(...) or hasAuthority('ACTOR_SELLER_TERMINAL')`.
- [x] All `Cashier*` controllers: `hasAuthority('ACTOR_SELLER_TERMINAL') or hasAnyRole(...)`.
- [x] CASHIER and AGENT authority strings removed from all `@PreAuthorize` annotations.

### 11.4 Permission checks on sensitive actions

- [x] `hasPermission('terminal.sell')` on sell and sale preparation.
- [x] Ticket cancel: `hasPermission('terminal.sell') or hasAnyRole(...)`.

### 11.5 Controller rules

- [x] Do not read tenantId from body for tenant/admin endpoints.
- [x] Use `@CurrentContext TchRequestContext`.
- [x] Keep controllers thin.
- [x] Keep handlers responsible for business invariants.
- [x] Add audit on sensitive actions.

## 12. Bootstrap feature

### 12.1 Structure

- [x] `publicruntime/` package with `PublicBootstrapRuntimeController` + `PublicRuntimeBootstrapService`.
- [x] `privateruntime/` package with `PrivateBootstrapRuntimeController`.

### 12.2 Public bootstrap

- [x] `GET /public/bootstrap` (+ legacy `/public/runtime/bootstrap` alias): no auth, returns settings + theme + i18n + pageModelRef.
- [x] No notices, no user state.
- [x] Theme calls `TenantThemeApi.resolveTenantThemeRuntime(null, "light")` → global default.

### 12.3 Private bootstrap — AppUser admin

- [x] `GET /tenant/me/bootstrap` (+ aliases `/runtime/private`, `/tenant/runtime/bootstrap`): auth required.
- [x] `GET /tenant/runtime/state`: lightweight state refresh.
- [x] Auth: `hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN') or hasAuthority('ACTOR_SELLER_TERMINAL')`.

### 12.4 Terminal bootstrap / me

- [ ] `GET /tenant/terminal/me` — deferred; depends on `seller-terminal-v0` (SellerTerminal DB tables and AccessResolutionFilter Slice 4).

## 13. End-to-end tests

- [x] `/public/**` works without token. (SecurityConfigRouteTest)
- [x] `/tenant/**` without token returns `401`. (SecurityConfigRouteTest)
- [ ] provider claim role=`SUPER_ADMIN` gives no business access. (requires live DB)
- [ ] AppUser tenant normal receives tenant and roles from DB. (requires live DB)
- [ ] tenant admin can access `/admin/**`. (requires live DB)
- [ ] super admin can access `/platform/**`. (requires live DB)
- [ ] super admin override tenant is audited. (requires live DB)
- [ ] business tables remain isolated by RLS. (requires live DB)
- [ ] Firebase technical terminal user resolves to `SELLER_TERMINAL`. (requires live Firebase + seller-terminal-v0 tables)
- [ ] active seller terminal can call terminal `me`. (requires seller-terminal-v0 tables)
- [ ] active seller terminal can sell only through terminal sale endpoint. (requires seller-terminal-v0 tables)
- [ ] blocked seller terminal cannot sell. (requires seller-terminal-v0 tables)
- [ ] seller terminal cannot access admin endpoints. (requires seller-terminal-v0 tables)
- [ ] `./mvnw verify` passes. (pre-existing ArchUnit failures in CleanArch/FeatureArch/PageModelArch/FlywayAuditAlignment remain; TenantConfigValidatorTest pre-existing)

## 14. Pipeline stabilization — Filter→Step + anchoring (added 2026-06-15)

> Refactor of slices 1–10: stabilize Spring filter ordering without anchoring custom filters on
> custom filters, and split identity/access resolution (pipeline filter) from context binding
> (`TchContextFilter`). See design.md "Pipeline stabilization — 2026-06-15".

### 14.1 SecurityConfig — filter anchoring

- [x] Anchor `tchAccessContextPipelineFilter` `addFilterAfter(BearerTokenAuthenticationFilter.class)`.
- [x] Anchor `sensitiveIdentityVerificationFilter` `addFilterAfter(BearerTokenAuthenticationFilter.class)`.
- [x] Anchor `tchContextFilter` `addFilterBefore(AuthorizationFilter.class)`.
- [x] Remove every custom-filter anchor (no `addFilterAfter(..., <custom>.class)`).
- [x] `convert()` emits empty authorities; `ExternalAuthenticatedUser` set as auth details.
- [x] Recreate `SecurityFilterRegistrationConfig` as a dedicated `@Configuration` with disabled
      `FilterRegistrationBean` for **both** `TchContextFilter` and `TchAccessContextPipelineFilter`
      (both are Spring `@Bean`s). Do **not** add one for `SensitiveIdentityVerificationFilter` while
      it is created with `new` in `SecurityConfig`.
- [x] Rule: any filter both manually added to the chain **and** a Spring bean has a disabled registration.

### 14.2 TchAccessContextPipelineFilter

- [x] Runs only `identityBootstrapStep.bootstrap` + `accessResolutionStep.resolve`.
- [x] `shouldNotFilter` covers public/health/swagger/openapi/error paths.
- [x] Binds no `TchRequestContext`, MDC, or RLS.

### 14.3 EffectiveTenantResolver (net-new, full behavior) — `platform.accesscontrol`

- [x] APP_USER (normal): tenant always from the single active membership; a request header never selects the tenant. Zero memberships → none; more than one → `403 tenant.ambiguous_membership`. `X-Tenant-Id` is not read.
- [x] SUPER_ADMIN without override: `effectiveTenantId=null`, `tenantOverride=false`.
- [x] SUPER_ADMIN override (all required): `ROLE_SUPER_ADMIN` + `PERM_platform.tenant.override` + `X-Tch-Tenant-Override` valid UUID + non-blank `X-Tch-Override-Reason` → `effectiveTenantId=target`, `tenantOverride=true`, audited. The override header is the only header-driven tenant selection. Target tenant ACTIVE enforced downstream by `hydrateResolvedTenant`.
- [x] Override decision (header + reason presence) owned by `EffectiveTenantResolver`; `TchContextFilter` re-checks reason as guard-rail only.
- [x] SELLER_TERMINAL: tenant from `BootstrappedActor`, never from headers (unchanged in `resolveSellerTerminal`).
- [x] Wired into `AccessResolutionStep`; raw header-only tenant parsing removed.

### 14.3a Seed `platform.tenant.override` permission

- [x] Add `platform.tenant.override` permission and grant it to `SUPER_ADMIN` (distinct from `platform.tenant.manage`).
- [x] Amended V232 in place (unmerged on this branch, not on `main`). Local/CI DBs that already ran V232 need `flyway repair` or a dev-DB reset.

### 14.4 TchContextFilter flow split

- [x] Early split on `RESOLVED_ACCESS`: `handleResolvedAccess` vs `handlePublicOrLegacy`.
- [x] Protected flow: tenant-required guard (TENANT/ADMIN), override-reason guard, `createFromResolvedAccess`, `hydrateResolvedTenant`, bind, operational context, bind.
- [x] Protected flow never re-resolves tenant from headers.
- [x] Context cleared in `finally`.

### 14.5 TenantContextResolver.hydrateResolvedTenant

- [x] Metadata-only (code/uuid/timezone/currency) via `tenantLookup.findById`.
- [x] 403 + null on not-found; no membership/permission decisions.
- [x] `resolveForScope` retained for public/legacy.

### 14.6 TchRequestContextFactory cleanup

- [x] Add `createFromResolvedAccess(req, scope, resolved)` and `createPublic(req, defaultTenantCode, scope)`.
- [x] `create(...)` becomes a thin alias to `createPublic(...)`.
- [x] Remove `AuthContextExtractor` field; delete `common.context.auth.AuthContextExtractor` (+ `ExtractedAuthContext`); no references remain.

### 14.7 Boundary + regression

- [x] ArchUnit: `common.context` imports none of the business layers (`CleanArchitectureRulesTest#commonDoesNotDependOnBusinessLayers` green).
- [x] `AccessResolutionFilterImplTest` / `UserBootstrapFilterImplTest` retired (deleted with the Filter→Step rename).
- [x] App boots with no `registered order` exception; `SecurityConfigRouteTest` green (5/5).
- [x] `EffectiveTenantResolver` unit tests — 10/10 (membership-only normal user, header ignored, ambiguous, super-admin override valid/invalid).
- [x] `TchContextFilterSlice5Test` updated for the flow split (7/7).

## 15. AccessResolutionStep — access-snapshot optimization (added 2026-06-15)

> Reduce per-request DB round-trips during authorization. `AccessResolutionStepImpl` becomes HTTP
> orchestration only; role/permission resolution moves to a batch-query `AccessControlSnapshotResolver`.

### 15.1 Batch snapshot queries — no N+1

- [x] `TenantUserRoleJpaRepository.findPlatformRoleAccessRows(userId)` — one join (`tenant_user_role ⨝ app_role ⨝ role_permission`, left join) returning `RoleAccessRow(roleCode, permissionCode)` for active PLATFORM roles.
- [x] `findTenantRoleAccessRows(tenantId, userId)` — same shape for active tenant roles.
- [x] Replaces: `findActivePlatformRoleIdsByUser` + `findAllById`, the per-role `listPermissionCodes` loops (×2), and the second `findAllById(tenant roleIds)`.

### 15.2 AccessControlSnapshotResolver

- [x] `resolvePlatform(userId) → PlatformAccess(superAdmin, roleCodes, permissionKeys)` (1 query).
- [x] `resolveTenant(userId, tenantId) → TenantAccess(roleCodes, permissionKeys)` (1 query + 1 overrides query); applies GRANT/DENY overrides, DENY wins.
- [x] Leaves `EffectivePermissionService` / `PermissionCatalogAdminAdapter` (and their `@Cacheable`) intact for their other callers.

### 15.3 Step is orchestration only

- [x] `AccessResolutionStepImpl` drops `EffectivePermissionService`, `TenantUserRoleJpaRepository`, `AppRoleJpaRepository`, `PermissionCatalogAdminAdapter`; depends only on `AccessControlSnapshotResolver` + `EffectiveTenantResolver`.
- [x] Per-request DB calls for a tenant user: ~4 (was ~6), no N+1, no redundant role-entity lookups.

### 15.4 Tests

- [x] `AccessControlSnapshotResolverTest` — 5/5 (platform collect + super-admin detect, role-without-perms, empty, tenant GRANT/DENY, DENY-wins).
- [x] JPQL projections parse at Hibernate startup (`SecurityConfigRouteTest` boots green); accesscontrol suite 25/25.
- [ ] Query **execution** (ad-hoc entity join) proven only by live-DB e2e (§13) — no `@DataJpaTest` harness in `platform`.

## 16. Identity bootstrap — client-type hint, no fallback (added 2026-06-15)

> `/tenant/**` is shared by `APP_USER` (web/admin) and `SELLER_TERMINAL` (POS). The path cannot
> determine the actor type, so the `X-Tch-Client-Type: POS` hint selects which resolver to use. The
> header selects a resolver only — it grants nothing; proof remains the verified JWT + DB mapping.

### 16.1 Client-type header + resolver

- [x] `TchHeaders.X_TCH_CLIENT_TYPE` (+ `CLIENT_TYPE_POS` / `CLIENT_TYPE_WEB`).
- [x] `ExpectedActorTypeResolver`: `POS` → `SELLER_TERMINAL`, else `APP_USER`.

### 16.2 UserBootstrapFilterImpl — no fallback

- [x] `POS` → SellerTerminal resolver only; absent → AppUser resolver only. No cross-fallback.
- [x] POS + no terminal mapping → `403 terminal.external_identity_not_linked` (does not try AppUser).
- [x] no POS + no AppUser mapping → `403 external_identity.not_linked` (does not try terminal).
- [x] Saves one DB lookup for POS requests (no AppUser miss first).

### 16.3 AccessResolutionStep — terminal tenant rules

- [x] `resolveSellerTerminal` takes the request and rejects `X-Tenant-Id` / `X-Tch-Tenant-Override` with `403 terminal.tenant_selection_not_allowed` (terminal is tenant-bound, not tenant-selecting).
- [x] Effective tenant always from `BootstrappedActor.tenantId()`; terminal permissions unchanged.
- [x] `ApiScope` unchanged (no `TERMINAL`): `/tenant/**` stays `ApiScope.TENANT`, actor type is orthogonal.

### 16.4 Tests

- [x] `UserBootstrapFilterImplTest` 5/5 (POS active / no-mapping / inactive; non-POS active / no-mapping; no-fallback verified via `verifyNoInteractions`).
- [x] `AccessResolutionStepSellerTerminalTest` 3/3 (tenant from actor + terminal perms; `X-Tenant-Id` and `X-Tch-Tenant-Override` each rejected).
- [x] Context boots with the new bean + changed constructor (`SecurityConfigRouteTest` 5/5).
- [x] Status rule preserved: bootstrap rejects inactive terminals early; the sale handler still revalidates `seller_terminal.status == ACTIVE` (deferred to `seller-terminal-v0`).
