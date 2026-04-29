# Permission Flow — Tchalanet Server

> **Status**: NORMATIVE  
> **Scope**: `tchalanet-server` — authorization, permissions, `core.accesscontrol`, Spring Method Security  
> **Audience**: backend developers, reviewers, agents IA  
> **Last reviewed**: 2026-04-28

---

## 1. Purpose

This document defines where authorization decisions live, how web endpoints express requirements, and how permission checks are evaluated uniformly.

It covers:

- `core.accesscontrol` responsibility;
- Spring Method Security integration;
- `TchPermissionEvaluator`;
- `CheckUserPermissionsQuery`;
- system roles to permissions strategy;
- V1 and V2 authorization model.

It does **not** define JWT authentication, tenant bootstrap or RLS context. See `authentication-flow.md`.

---

## 2. Core principle

Authorization decisions belong to `core.accesscontrol`.

The web layer expresses a requirement. It does not decide.

Consequences:

- controllers must not contain `if (role == ...)` authorization logic;
- controllers must not call repositories or security tables;
- controllers must declare requirements via annotations;
- `core.accesscontrol` evaluates whether a user can perform an action in a tenant context.

---

## 3. Current implementation summary

Current code already has:

- `TchRole` system roles:
  - `SUPER_ADMIN`,
  - `TENANT_ADMIN`,
  - `OPERATOR`,
  - `CASHIER`,
  - `SYSTEM`;
- `MethodSecurityConfig` registering `TchPermissionEvaluator`;
- `TchPermissionEvaluator` calling `CheckUserPermissionsHandler` directly;
- `RequiresPermissionAspect` calling `QueryBus`;
- `CheckUserPermissionsQuery` / `CheckUserPermissionsHandler` concept;
- `SecurityConfig` with method security enabled.

Known issues to fix:

- `TchPermissionEvaluator` currently expects `Authentication.getPrincipal()` to be `TchRequestContext`, but Spring Security currently uses a `JwtAuthenticationToken` whose principal is `Jwt`;
- both `@PreAuthorize(...hasPermission...)` and `@RequiresPermission` exist, causing two authorization entrypoints;
- evaluator should use `QueryBus`, not handler direct injection;
- tenant/user must come from `TchRequestContext`, not the Spring principal.

---

## 4. Canonical authorization pipeline

```text
Controller method
  ↓
@PreAuthorize("hasPermission('permission.key')")
  ↓
TchPermissionEvaluator
  ↓
TchContextResolver.currentOrThrow()
  ↓
QueryBus.send(CheckUserPermissionsQuery)
  ↓
core.accesscontrol
  ↓
allow / deny
```

---

## 5. Canonical Spring method-security style

### 5.1 Standard

The canonical style is Spring Method Security:

```java
@PreAuthorize("hasPermission('ticket.read')")
@GetMapping
public ApiResponse<TchPage<TicketResponse>> list(...) {
  ...
}
```

### 5.2 Optional meta-annotation

A meta-annotation may be introduced later for readability:

```java
@RequiredPermission("ticket.read")
```

If introduced, it must delegate to Spring Method Security and must not introduce a second custom authorization engine.

### 5.3 RequiresPermissionAspect

`RequiresPermissionAspect` is not the canonical path.

Target options:

1. remove it; or
2. mark it deprecated and keep only as migration compatibility.

No new controller should depend on it.

---

## 6. TchPermissionEvaluator

### 6.1 Responsibility

`TchPermissionEvaluator` is the single adapter between Spring Method Security and `core.accesscontrol`.

It must:

- normalize the permission key;
- read `TchRequestContext` through `TchContextResolver.currentOrThrow()`;
- extract typed `TenantId` and `UserId` from the context;
- call `QueryBus.send(new CheckUserPermissionsQuery(...))`;
- return `true` when allowed;
- return `false` when denied.

### 6.2 It must not

`TchPermissionEvaluator` must not:

- read `Authentication.getPrincipal()` as `TchRequestContext`;
- parse JWT claims;
- call repositories;
- bypass `QueryBus`;
- implement role/permission decisions itself;
- create ad-hoc caches.

### 6.3 Context extraction

Canonical target usage:

```java
var ctx = tchContextResolver.currentOrThrow();
TenantId tenantId = ctx.tenantIdSafe();
UserId userId = ctx.currentUserIdRequired();
```

For tenant-scoped permissions, `tenantId` must be present.

For platform-only permissions, the query model may allow `tenantId = null` if the permission is explicitly global/platform-scoped.

### 6.4 Deny behavior

Domain/application code may throw `PermissionsDeniedException`.

The evaluator catches domain deny exceptions and returns `false`.

Spring Security then converts the method-security denial into an access-denied response.

---

## 7. Query contract

### 7.1 Canonical query

```java
CheckUserPermissionsQuery(
  TenantId tenantId,
  UserId userId,
  Set<String> permissions
)
```

Target improvement:

- replace raw strings internally with a typed permission value object such as `PermissionKey`;
- keep external annotation values as strings for readability.

### 7.2 Result

The handler should return a boolean decision or a decision object.

Recommended V1:

```java
boolean allowed
```

Recommended V2:

```java
PermissionDecision(
  boolean allowed,
  Set<PermissionKey> granted,
  Set<PermissionKey> missing,
  String reasonCode
)
```

The web/evaluator path must only expose allow/deny behavior, not internal decision details.

---

## 8. Role model

### 8.1 System roles

`TchRole` contains platform/system-level roles:

```java
SUPER_ADMIN
TENANT_ADMIN
OPERATOR
CASHIER
SYSTEM
```

These roles come from Keycloak and are stored in `TchRequestContext.systemRoles()`.

### 8.2 Roles are not permissions

Roles are coarse identity attributes.

Permissions are action-level requirements.

Examples:

```text
Role:       CASHIER
Permission: sale.create
Permission: ticket.print
Permission: session.open
```

A controller should require permissions, not roles.

---

## 9. Permission key conventions

Permission keys should be stable, human-readable and namespaced.

Canonical format:

```text
<domain>.<action>
```

Examples:

```text
ticket.read
ticket.print
sale.create
payout.approve
outlet.admin
terminal.admin
session.open
session.close
report.read
tenant.config.manage
platform.tenant.read
platform.tenant.manage
```

Rules:

- lowercase;
- dot-separated namespaces;
- no accents;
- no tenant/user IDs embedded in permission keys;
- no UI labels as permission keys.

---

## 10. V1 strategy — role-derived permissions

V1 may derive permissions from system roles without full DB-driven RBAC.

Example baseline:

| Role           | Permissions                                        |
| -------------- | -------------------------------------------------- |
| `SUPER_ADMIN`  | all permissions, including `platform.*`            |
| `TENANT_ADMIN` | tenant/admin permissions                           |
| `OPERATOR`     | outlet/session/report/sale operational permissions |
| `CASHIER`      | sale/ticket/session basic permissions              |

Example mapping:

```text
SUPER_ADMIN
  → *

TENANT_ADMIN
  → tenant.config.manage
  → user.manage
  → outlet.admin
  → terminal.admin
  → report.read

OPERATOR
  → sale.create
  → ticket.read
  → ticket.print
  → session.open
  → session.close
  → report.read

CASHIER
  → sale.create
  → ticket.read
  → ticket.print
  → session.open
  → session.close
```

The mapping lives in `core.accesscontrol`, not in controllers or `SecurityConfig`.

---

## 11. V2 strategy — DB-driven permissions

V2 may introduce DB-driven RBAC.

Candidate tables:

```text
permission
app_role
role_permission
tenant_user_role
```

Rules:

- DB is source of truth for custom tenant roles;
- Keycloak remains source of authentication and system role bootstrap;
- `CheckUserPermissionsQuery` contract should remain stable;
- changing role assignments must invalidate permission cache after commit.

---

## 12. Cache rules

Permission cache is allowed as an optimization only.

It must:

- use `CacheSpecProvider`;
- be keyed by tenant/user/role version where appropriate;
- be evicted after role/permission changes commit;
- never become source of truth.

It must not:

- be implemented ad hoc in `TchPermissionEvaluator`;
- hide authorization changes indefinitely;
- bypass `core.accesscontrol`.

---

## 13. Controller rules

Controllers must:

- use `@PreAuthorize("hasPermission('...')")` for protected actions;
- keep business logic out;
- inject `@CurrentContext TchRequestContext` when needed for request mapping;
- delegate to `CommandBus` / `QueryBus`.

Controllers must not:

- check roles manually;
- check permissions manually;
- read JWT claims;
- read `SecurityContextHolder`;
- trust tenant IDs from request bodies as source of truth.

---

## 14. SecurityConfig role

`SecurityConfig` is only a coarse gate.

It may require:

- public endpoints are public;
- tenant/admin endpoints are authenticated;
- platform endpoints require `SUPER_ADMIN`.

It must not contain business permission mapping.

Target:

```java
.requestMatchers("/api/v1/admin/**").authenticated()
.requestMatchers("/api/v1/tenant/**").authenticated()
.requestMatchers("/api/v1/platform/**").hasRole("SUPER_ADMIN")
```

Then method-level permissions decide individual actions.

---

## 15. Tenant safety

The effective tenant comes from `TchRequestContext`.

It may include super-admin override already resolved by `TchContextFilter`.

Permission checks must not accept tenant IDs from the client as authority.

For tenant-scoped endpoints:

- missing tenant must be blocked before controller or permission handler;
- permission evaluation should assume tenant context is present.

---

## 16. Platform permissions

Platform permissions are allowed only for `SUPER_ADMIN` flows.

Examples:

```text
platform.tenant.read
platform.tenant.manage
platform.cache.clear
platform.audit.read
```

Platform permission checks may be global and not tenant-scoped.

The query model must explicitly support this distinction if needed.

---

## 17. Relation to autonomy and limits

Access control answers:

```text
Can this user attempt this action?
```

It does not replace:

- limit-policy evaluation;
- approval workflows;
- payout rules;
- sales validation;
- autonomy escalation rules.

Autonomy may inform authorization, but business thresholds and approvals remain in their own core domains.

Example:

```text
permission sale.create = user may attempt sale
limit policy = sale amount may require approval or be blocked
```

---

## 18. Testing checklist

- [ ] evaluator uses `TchContextResolver`, not Spring principal;
- [ ] deny returns `false` from evaluator;
- [ ] handler grants expected V1 permissions by role;
- [ ] handler denies missing permission;
- [ ] super-admin wildcard works;
- [ ] tenant missing fails on tenant-scoped permission;
- [ ] controller has annotation and no manual `if role` logic.

---

## 19. PR checklist

- [ ] controller expresses requirements via `@PreAuthorize`;
- [ ] no manual authorization decisions in controller;
- [ ] evaluator calls `QueryBus`;
- [ ] tenant/user come from `TchRequestContext`;
- [ ] no direct repository access in evaluator;
- [ ] role-to-permission logic lives in `core.accesscontrol`;
- [ ] tests cover allow and deny.
