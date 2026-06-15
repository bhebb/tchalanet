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

- [ ] `/public/**` -> `permitAll`.
- [ ] `/actuator/health/**` -> `permitAll`.
- [ ] everything else -> `authenticated`.
- [ ] Remove business rules from global SecurityConfig:
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

- [ ] `/public/**` works without token.
- [ ] `/actuator/health/**` works without token.
- [ ] `/tenant/**` without token returns `401`.
- [ ] `/admin/**` without token returns `401`.
- [ ] `/platform/**` without token returns `401`.

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

- [ ] List existing policies.
- [ ] Remove RLS policies only from approved bootstrap tables.
- [ ] Do not remove RLS from business tenant-scoped tables.
- [ ] Document the reason:
  - bootstrap tables are required to resolve actor, AppUser, terminal, tenant, roles, and permissions before context exists;
  - they are protected by module boundaries, repositories, constraints, status checks, and audit.

### 3.3 Migration

- [ ] Create Flyway migration only after table classification is approved.
- [ ] Do not include `seller_terminal` business profile in the bootstrap exception unless explicitly approved.

## 4. Seeds — roles and permissions

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

### 5.2 Actor type

In `common.context` or `common.security`, create:

```java
public enum TchActorType {
    APP_USER,
    SELLER_TERMINAL,
    SYSTEM
}
```

### 5.3 BootstrappedActor

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

### 5.5 ResolvedAccessContext

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

### 5.6 Request attributes

Create constants:

```java
BOOTSTRAPPED_ACTOR
RESOLVED_ACCESS
TENANT_OVERRIDE
```

## 6. IdentityBootstrapFilter

### 6.1 Responsibilities

- [ ] Read technical external principal already authenticated by provider verification.
- [ ] Resolve `(provider, issuer, externalSubject)`.
- [ ] Try AppUser external identity.
- [ ] If found:
  - load AppUser;
  - verify status `ACTIVE`;
  - attach `BootstrappedActor(APP_USER, ...)`.
- [ ] Else try SellerTerminal external identity.
- [ ] If found:
  - verify identity record status;
  - attach `BootstrappedActor(SELLER_TERMINAL, ...)`.
- [ ] Else deny according to accepted identity policy.

### 6.2 Non-responsibilities

The filter must not:

- resolve AppUser tenant access;
- load AppUser roles;
- load AppUser permissions;
- bind RLS;
- construct `TchRequestContext`;
- execute sale terminal business validation.

### 6.3 Errors

- [ ] Invalid provider identity: `401`.
- [ ] Unlinked external identity: `401` or `403` per identity policy.
- [ ] AppUser disabled/locked: `403`.
- [ ] SellerTerminal identity disabled/invalid: `403`.

### 6.4 Tests

- [ ] active AppUser passes.
- [ ] disabled AppUser is blocked before controller.
- [ ] unknown external identity is blocked.
- [ ] provider mismatch is blocked.
- [ ] SellerTerminal external identity resolves actor type `SELLER_TERMINAL`.
- [ ] SellerTerminal disabled identity is blocked.

## 7. AccessResolutionFilter

### 7.1 Responsibilities

- [ ] Read `BOOTSTRAPPED_ACTOR`.
- [ ] Resolve HTTP scope:
  - `PUBLIC`
  - `TENANT`
  - `ADMIN`
  - `PLATFORM`
- [ ] For `APP_USER`, call `AccessControlApi`.
- [ ] For `SELLER_TERMINAL`, resolve terminal effective tenant and derived terminal permissions.
- [ ] Validate `SUPER_ADMIN` tenant override when requested.
- [ ] Attach `RESOLVED_ACCESS`.
- [ ] Enrich Spring Authentication with:
  - `ROLE_*`
  - `PERM_*`
  - `ACTOR_*`.

### 7.2 AppUser tenant flow

- [ ] Load exactly one active `tenant_user`.
- [ ] Load `tenant_user_role`.
- [ ] Load permissions via `app_role_permission`.
- [ ] Apply `tenant_user_permission_override`.
- [ ] Remove permissions explicitly `DENY`.
- [ ] Return tenant + roles + permissions.

### 7.3 Super admin flow

- [ ] Load `platform_user_role`.
- [ ] `/platform/**` without override:
  - effective tenant = null;
  - role = `SUPER_ADMIN`.
- [ ] with override:
  - verify `SUPER_ADMIN`;
  - verify tenant active;
  - audit override;
  - effective tenant = target tenant;
  - roles = `SUPER_ADMIN` + tenant override set.

### 7.4 SellerTerminal flow

`AccessResolutionFilter` reads only `seller_terminal_external_identity` (bootstrap-safe). It SHALL
NOT access the full RLS-protected `seller_terminal` business table before `TchRequestContext` is
bound. Full status (ACTIVE/BLOCKED/DISABLED) is enforced by the business handler.

- [ ] Verify terminal identity mapping in `seller_terminal_external_identity`.
- [ ] Resolve effective tenant from mapping.
- [ ] Attach `ACTOR_SELLER_TERMINAL` to Spring Authentication.
- [ ] Attach pre-context terminal permissions (optimistic grants; handler is authoritative gate):
  - `PERM_terminal.me.read`
  - `PERM_terminal.sell`
  - `PERM_terminal.ticket.read_own`
  - `PERM_terminal.ticket.reprint_own`
- [ ] If identity mapping is disabled/invalid: `403`.

### 7.5 Spring authorities

Map:

```text
TENANT_OWNER        -> ROLE_TENANT_OWNER
TENANT_ADMIN        -> ROLE_TENANT_ADMIN
SUPER_ADMIN         -> ROLE_SUPER_ADMIN
APP_USER            -> ACTOR_APP_USER
SELLER_TERMINAL     -> ACTOR_SELLER_TERMINAL

ticket.void         -> PERM_ticket.void
terminal.sell       -> PERM_terminal.sell
terminal.block      -> PERM_terminal.block
draw_result.confirm -> PERM_draw_result.confirm
```

### 7.6 Tests

- [ ] tenant admin receives `ROLE_TENANT_ADMIN`.
- [ ] tenant owner receives `ROLE_TENANT_OWNER`.
- [ ] permission DENY removes authority.
- [ ] super admin platform receives `ROLE_SUPER_ADMIN` without tenant.
- [ ] super admin override receives effective tenant + expected roles.
- [ ] user without active membership is refused on `/tenant/**` and `/admin/**`.
- [ ] seller terminal receives `ACTOR_SELLER_TERMINAL`.
- [ ] active seller terminal receives `PERM_terminal.sell`.
- [ ] blocked seller terminal does not receive `PERM_terminal.sell`.

## 8. TchContextFilter / canonical context binder

### 8.1 Responsibilities

- [ ] Read `RESOLVED_ACCESS`.
- [ ] Add request id / trace id.
- [ ] Add locale / timezone.
- [ ] Build actor-neutral `TchRequestContext`:
  - `actorType`;
  - `appUserId` nullable;
  - `sellerTerminalId` nullable;
  - `effectiveTenantId` nullable for platform;
  - roles;
  - permissions.
- [ ] Resolve operational context if present:
  - terminal;
  - outlet;
  - sales session.
- [ ] For `SELLER_TERMINAL`, set operational terminal context from `sellerTerminalId`.
- [ ] Bind:
  - request attribute;
  - ThreadLocal;
  - MDC;
  - RLS datasource facts.
- [ ] Clear in `finally`.

### 8.2 Rules

- [ ] `TchContextFilter` must not import `platform.identity`.
- [ ] `TchContextFilter` must not import `platform.accesscontrol`.
- [ ] `TchContextFilter` must not import `core.terminal`.
- [ ] `TchContextFilter` consumes only neutral common.context contracts.
- [ ] After this filter, application code reads only `TchRequestContext`.

### 8.3 Tests

- [ ] context bound on protected request.
- [ ] context cleared after request.
- [ ] tenant/admin without effective tenant blocks before controller.
- [ ] platform super admin without override can have tenant null.
- [ ] SellerTerminal context has actorType and terminal id.
- [ ] RLS receives expected tenant.

## 9. TchPermissionEvaluator

### 9.1 Implementation V1

- [ ] Read `TchRequestContext`.
- [ ] Normalize requested permission.
- [ ] Return `false` if context absent.
- [ ] Return `false` if permission absent.
- [ ] Return `true` if `ctx.permissions()` contains requested permission.
- [ ] Do not call repositories directly.
- [ ] Do not read JWT/provider token.

### 9.2 Tests

- [ ] permission present returns true.
- [ ] permission absent returns false.
- [ ] context absent returns false.
- [ ] permission DENY returns false through resolved permissions.
- [ ] terminal permission `terminal.sell` works for active terminal.

## 10. Batch and scheduler context

### 10.1 Batch context binder

- [ ] Create or adjust `BatchTchContextBinder`.
- [ ] Support actor type `SYSTEM`.
- [ ] Support context `TENANT`.
- [ ] Support context `PLATFORM`.
- [ ] Bind before DB access.
- [ ] Clear in `finally`.

### 10.2 Rules

- [ ] Batch does not pass through `IdentityBootstrapFilter`.
- [ ] Batch does not pass through `AccessResolutionFilter`.
- [ ] Batch does not pass through HTTP filters.
- [ ] Batch uses actor `SYSTEM`.
- [ ] Batch uses explicit system authorities allowlisted by job family.

### 10.3 Tests

- [ ] batch tenant has effective tenant.
- [ ] batch platform has platform scope.
- [ ] batch context is cleared after execution.
- [ ] no context leak between jobs.

## 11. Controllers — migration by surfaces

### 11.1 Inventory

- [ ] List controllers under `/tenant/**`.
- [ ] List controllers under `/admin/**`.
- [ ] List controllers under `/platform/**`.
- [ ] List terminal seller controllers/surfaces.
- [ ] List sensitive actions:
  - sell;
  - void;
  - payout mark paid;
  - result create/confirm/correct;
  - terminal create/block/unblock/disable/reset access;
  - odds manage;
  - limit manage;
  - tenant override;
  - ops force actions.

### 11.2 Role checks on admin surfaces

Examples:

```java
@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
```

For platform:

```java
@PreAuthorize("hasRole('SUPER_ADMIN')")
```

### 11.3 Actor checks on terminal surfaces

Examples:

```java
@PreAuthorize("hasAuthority('ACTOR_SELLER_TERMINAL')")
```

For sale:

```java
@PreAuthorize("hasPermission('terminal.sell')")
```

### 11.4 Permission checks on sensitive actions

Examples:

```java
@PreAuthorize("hasPermission('ticket.void')")
@PreAuthorize("hasPermission('terminal.block')")
@PreAuthorize("hasPermission('draw_result.confirm')")
@PreAuthorize("hasPermission('payout.mark_paid')")
@PreAuthorize("hasPermission('odds.manage')")
@PreAuthorize("hasPermission('limit.manage')")
```

### 11.5 Controller rules

- [ ] Do not read tenantId from body for tenant/admin endpoints.
- [ ] Use `@CurrentContext TchRequestContext`.
- [ ] Keep controllers thin.
- [ ] Keep handlers responsible for business invariants.
- [ ] Add audit on sensitive actions.

## 12. Bootstrap feature

### 12.1 Structure

Create or align:

```text
features.bootstrap
  public/
  private/
```

### 12.2 Public bootstrap

Endpoint:

```http
GET /public/bootstrap
```

Return minimal:

```text
public theme
locale
public navigation
public feature flags
```

### 12.3 Private bootstrap — AppUser admin

Endpoint:

```http
GET /tenant/me/bootstrap
```

Return minimal:

```text
actor type
app user summary
tenant summary
roles
permissions
default route
navigation
operational context status
notices
```

### 12.4 Terminal bootstrap / me

Endpoint:

```http
GET /tenant/terminal/me
```

Return minimal:

```text
actor type SELLER_TERMINAL
terminal summary
tenant summary
terminal status
commission rate
allowed terminal actions
notices
```

## 13. End-to-end tests

- [ ] `/public/**` works without token.
- [ ] `/tenant/**` without token returns `401`.
- [ ] provider claim role=`SUPER_ADMIN` gives no business access.
- [ ] AppUser tenant normal receives tenant and roles from DB.
- [ ] tenant admin can access `/admin/**`.
- [ ] super admin can access `/platform/**`.
- [ ] super admin override tenant is audited.
- [ ] business tables remain isolated by RLS.
- [ ] Firebase technical terminal user resolves to `SELLER_TERMINAL`.
- [ ] active seller terminal can call terminal `me`.
- [ ] active seller terminal can sell only through terminal sale endpoint.
- [ ] blocked seller terminal cannot sell.
- [ ] seller terminal cannot access admin endpoints.
- [ ] `./mvnw verify` passes.
