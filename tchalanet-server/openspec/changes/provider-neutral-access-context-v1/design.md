# Design: Provider-Neutral Access Context V1

## Decisions

### seller_terminal_external_identity FK

`seller_terminal_external_identity` requires a FK to `seller_terminal`. A bare
`seller_terminal_id UUID` without a constraint is not acceptable.

Decision: defer both `seller_terminal` and `seller_terminal_external_identity` entirely to
`seller-terminal-v0`. This change does not create either table. SellerTerminal actor resolution
in the authentication pipeline is implemented but tested with stubs/mocks in unit tests;
integration tests against a real `SELLER_TERMINAL` identity flow require `seller-terminal-v0` to
have landed first.

### Terminal status in access resolution

`AccessResolutionFilter` SHALL read only `seller_terminal_external_identity` (bootstrap-safe) to
resolve the terminal actor and effective tenant. It SHALL NOT access the full RLS-protected
`seller_terminal` business table before `TchRequestContext` and RLS are bound.

Full terminal status (ACTIVE / BLOCKED / DISABLED) SHALL be validated inside the business handler
after `TchRequestContext` and RLS binding. The derived terminal permissions attached by
`AccessResolutionFilter` (`terminal.sell`, etc.) are pre-context optimistic grants; the handler
is the authoritative gate on terminal operational state.

### TchRole migration strategy

The target `TchRequestContext` uses:

```text
actorType: TchActorType
roleCodes: Set<String>
permissionKeys: Set<String>
```

`Set<TchRole>` is kept temporarily for backward compatibility during the migration. `SELLER_TERMINAL`
is not a role; it is represented by `actorType = SELLER_TERMINAL`. New code SHALL read `actorType`
and `roleCodes`/`permissionKeys`. Legacy `TchRole` accessors may remain as deprecated bridges until
all callers are migrated.

### platform_user_role — no separate table

A separate `platform_user_role` table does not exist and will not be created in this change.
Platform-scoped role assignments live in `tenant_user_role` alongside tenant-scoped ones.
`AccessResolutionFilter` distinguishes platform vs tenant roles by joining `app_role` and
filtering on `app_role.scope = 'PLATFORM'`.

### AccessResolutionFilter module boundary

Resolution logic (AppUser access, super-admin override, SellerTerminal terminal/tenant resolution)
lives in `platform.accesscontrol`. The servlet filter class and filter registration live in
`tchalanet-app`. The `tchalanet-app` filter delegates to `platform.accesscontrol` and converts
the result to `ResolvedAccessContext` from `common.context`.

### SensitiveIdentityVerificationFilter audit requirement

Before Slice 3, `SensitiveIdentityVerificationFilter` must be audited:

- If it verifies provider-token facts only (Spring `Authentication` principal), extend it to
  support SellerTerminal technical users at the provider level.
- If it depends on `AppUser`, tenant, roles, or permissions, it must be split: provider-level
  verification stays before `IdentityBootstrapFilter`; actor/tenant-level verification moves to
  after access resolution.

The audit outcome must be recorded before Slice 3 merges.

### Provider boundary

The configured authentication provider SHALL only validate credentials and produce a technical authenticated principal containing normalized provider, issuer, external subject, and safe profile claims.

Provider roles, groups, permissions, tenant claims, and custom business claims SHALL NOT become Tchalanet business authorities.

### Application sources of truth

```text
Identity mapping and AppUser status       -> platform.identity
AppUser roles and permissions             -> platform.accesscontrol
SellerTerminal identity and status         -> core.terminal
Runtime request context                    -> common.context
Filter ordering and assembly               -> tchalanet-app
Business invariants                        -> owning core domain
```

### Actor model

V1 supports these runtime actor types:

```java
public enum TchActorType {
    APP_USER,
    SELLER_TERMINAL,
    SYSTEM
}
```

Rules:

- `APP_USER` is a human/admin actor mapped from provider identity to `AppUser`.
- `SELLER_TERMINAL` is an operational seller terminal mapped from provider identity to `SellerTerminal`.
- `SYSTEM` is reserved for batch/scheduler contexts.
- A SellerTerminal is not a tenant admin user.
- A SellerTerminal may use a Firebase technical user for authentication, but business access is controlled by Tchalanet.

### V1 tenant model

- A normal AppUser has exactly one active tenant membership.
- A `SUPER_ADMIN` is platform-scoped by default.
- A `SUPER_ADMIN` may enter tenant scope only through an explicit, validated, audited override.
- A SellerTerminal always has an effective tenant.
- Ambiguous, missing, disabled, blocked, or locked access facts deny the request.

### Authorization semantics

- A role opens a human/admin functional surface.
- A permission authorizes attempting a sensitive human/admin action.
- A SellerTerminal receives only terminal-scoped derived authorities.
- Effective AppUser permissions are computed by `platform.accesscontrol`; explicit `DENY` wins.
- Controllers use class/surface role checks and method/action permission checks.
- Core handlers continue to enforce resource state and business invariants.

## Canonical HTTP Pipeline

```text
BearerTokenAuthenticationFilter
  -> validates provider token
  -> creates technical Authentication only

IdentityBootstrapFilter
  -> resolves provider + issuer + external subject through Tchalanet mappings
  -> resolves actor as APP_USER or SELLER_TERMINAL
  -> validates basic actor status
  -> attaches BootstrappedActor as temporary request input

AccessResolutionFilter
  -> resolves scope, effective tenant, roles, permissions, actor authorities
  -> validates explicit SUPER_ADMIN tenant override
  -> enriches Spring Authentication with ROLE_*, PERM_*, ACTOR_*
  -> attaches ResolvedAccessContext as temporary request input

TchContextFilter / canonical context binder
  -> consumes neutral resolved-access facts
  -> adds request metadata and operational context
  -> binds TchRequestContext to request, ThreadLocal, and MDC

RlsAwareDataSource
  -> reads the bound TchRequestContext
  -> binds PostgreSQL RLS session variables
```

## Layer Boundary Resolution

`common.context` SHALL remain independent of `platform` and `core`.

`platform.identity`, `platform.accesscontrol`, and `core.terminal` may depend on neutral contracts from `common.context`, but `common.context` SHALL NOT import `platform.*`, `core.*`, or any module-specific model.

`AccessResolutionFilter` SHALL convert module output into a neutral `ResolvedAccessContext` owned by `common.context`.

`ResolvedAccessContext` is a temporary HTTP pipeline input. It is not the canonical runtime context. After `TchContextFilter` runs, application code SHALL read access facts from `TchRequestContext` only.

## Neutral Contracts

### BootstrappedActor

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

Invariants:

```text
APP_USER:
- appUserId required
- sellerTerminalId null
- tenantId may be null until access resolution

SELLER_TERMINAL:
- sellerTerminalId required
- tenantId required
- appUserId null

SYSTEM:
- not produced by HTTP identity bootstrap
```

### ResolvedAccessContext

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

Invariants:

```text
APP_USER:
- appUserId required
- sellerTerminalId null

SELLER_TERMINAL:
- sellerTerminalId required
- effectiveTenantId required
- appUserId null
- roleCodes empty unless a synthetic actor authority is needed
- permissionKeys limited to terminal actions

SYSTEM:
- allowed only for explicit batch/scheduler contexts
```

### Request Attributes

```java
public final class TchContextRequestAttributes {
    public static final String BOOTSTRAPPED_ACTOR = "tch.bootstrappedActor";
    public static final String RESOLVED_ACCESS = "tch.resolvedAccess";
    public static final String TENANT_OVERRIDE = "tch.tenantOverride";

    private TchContextRequestAttributes() {}
}
```

`tchalanet-app` owns filter registration and ordering.

## Identity Bootstrap

Identity bootstrap resolves:

```text
(provider, issuer, externalSubject) -> Tchalanet actor
```

Resolution order:

1. Try `platform.identity` AppUser external identity.
2. Try SellerTerminal external identity.
3. Deny if no known actor is found.

Identity bootstrap does not resolve AppUser permissions, operational context, RLS, or business authorizations.

Failures:

- invalid or mismatched provider identity: `401`;
- unlinked external identity: deny according to accepted identity policy;
- disabled/locked AppUser: `403`;
- disabled/blocked SellerTerminal: `403` for sale endpoints, but `me/status` may be allowed according to terminal policy.

## Data Model Classification

### Bootstrap identity/access tables

These are candidates for reviewed non-RLS exception because they are required before tenant context exists:

```text
app_user_external_identity
app_user minimal status lookup
tenant_user membership lookup
tenant_user_role
platform_user_role
app_role
permission
app_role_permission
tenant_user_permission_override
seller_terminal_external_identity
```

### Business/control tables

These remain tenant-scoped and RLS-protected:

```text
seller_terminal
ticket
ticket_line
payout
draw
draw_result
odds_profile
odds_rule
limit_profile
limit_rule
audit_log tenant-scoped
```

Rationale:

`seller_terminal_external_identity` is enough to resolve the terminal actor. The full `seller_terminal` profile contains PII, commission, status, odds/limit links, and business control data, so it should remain tenant-scoped and protected by RLS where feasible.

## Access Resolution

### AppUser flow

`platform.accesscontrol` returns an immutable snapshot containing:

```text
appUserId
effectiveTenantId nullable only for valid platform scope
superAdmin
tenantOverride
roleCodes
permissionKeys
```

Effective tenant rules by scope:

- `TENANT` and `ADMIN`: missing `effectiveTenantId` is denied before controllers.
- `PLATFORM`: `effectiveTenantId` is null unless an explicit tenant override is validated.
- `PUBLIC`: `effectiveTenantId` may be the configured public/default tenant.

Normal tenant flow:

1. Resolve exactly one active membership.
2. Load active role assignments and role permissions.
3. Apply active user permission overrides.
4. Remove every permission with explicit `DENY`.
5. Return effective tenant and authorities.

Super-admin flow:

1. Resolve platform role.
2. Without override, remain platform-scoped with no effective tenant.
3. With override, validate tenant and permission to override.
4. Record the override decision for audit.
5. Return effective tenant and configured override authority set.

A `SUPER_ADMIN` tenant override does not bypass authorization. For V1, the configured override authority set is initially equivalent to `TENANT_ADMIN`, while retaining platform audit metadata.

### SellerTerminal flow

SellerTerminal access resolution:

1. Read `BootstrappedActor` with `actorType = SELLER_TERMINAL`.
2. Validate the mapped terminal identity still exists.
3. Resolve effective tenant from terminal identity.
4. Load terminal status required for derived permissions.
5. Build terminal-scoped permissions.

Derived permissions:

```text
ACTIVE terminal:
- terminal.me.read
- terminal.sell
- terminal.ticket.read_own
- terminal.ticket.reprint_own

BLOCKED terminal:
- terminal.me.read only

DISABLED/PENDING terminal:
- deny sale; optional terminal status endpoint only
```

Every sale still revalidates the full `SellerTerminal` from the business table and must reject unless status is `ACTIVE`.

No tenant id from a request body is trusted for context resolution.

## Spring Security

Route authorization remains broad:

```text
/public/**           permitAll
/actuator/health/**  permitAll
everything else      authenticated
```

Business authorization lives in method security:

```java
@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
@PreAuthorize("hasPermission('terminal.block')")
@PreAuthorize("hasAuthority('ACTOR_SELLER_TERMINAL')")
@PreAuthorize("hasPermission('terminal.sell')")
```

Authority mapping:

```text
TENANT_ADMIN            -> ROLE_TENANT_ADMIN
TENANT_OWNER            -> ROLE_TENANT_OWNER
SUPER_ADMIN             -> ROLE_SUPER_ADMIN
APP_USER actor          -> ACTOR_APP_USER
SELLER_TERMINAL actor   -> ACTOR_SELLER_TERMINAL
ticket.sell             -> PERM_ticket.sell
terminal.sell           -> PERM_terminal.sell
```

`TchPermissionEvaluator` reads the current canonical context and returns `false` when context or permission facts are missing. It does not query repositories.

## RLS Bootstrap Exception

Identity/access reads needed to construct effective tenant context cannot rely on tenant RLS that requires that same context.

Before adding or changing migrations, the implementation SHALL inventory the actual tables and classify them:

- global/bootstrap lookup tables that require a reviewed non-RLS exception;
- tenant-scoped administration tables that can remain RLS-protected;
- business tables that MUST remain RLS-protected.

Any new non-RLS exception SHALL be approved and recorded in an ADR or normative RLS update before the migration is created. Internal module boundaries, controlled repositories, constraints, status checks, and audited mutations are mandatory compensating controls.

## Batch And Scheduler Context

Non-HTTP execution SHALL not invoke HTTP identity/access filters.

Batch and scheduler entry points create an explicit system context:

```text
actor = SYSTEM
scope = TENANT or PLATFORM
tenantId = required job parameter for tenant scope
roles/permissions = explicit approved system authorities
```

Batch system authorities SHALL be named and allowlisted per job family. There is no implicit all-powerful `SYSTEM` authority set.

The context is bound before database access and cleared in `finally`.

## Observability And Audit

- Request id and trace id remain part of canonical context and MDC.
- Tenant override attempts and outcomes are audited.
- Identity/access admin mutations are audited.
- SellerTerminal create/block/unblock/disable/reset access actions are audited.
- Provider-specific identifiers may appear in diagnostics, but not in business authorization contracts.

## Controller Migration Policy

- Admin surfaces use role checks: `TENANT_OWNER`, `TENANT_ADMIN`, `SUPER_ADMIN`.
- Sensitive admin actions use permissions: terminal block, odds manage, limit manage, result confirm, payout mark paid.
- Terminal seller surfaces use `ACTOR_SELLER_TERMINAL`.
- Terminal sale action uses `terminal.sell` plus core terminal status validation.
- Existing `CASHIER` role can remain as legacy compatibility but must not be used for new SellerTerminal flows.
