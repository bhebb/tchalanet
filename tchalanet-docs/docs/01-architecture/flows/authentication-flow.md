# Authentication Flow — Tchalanet Server

> **Status**: NORMATIVE  
> **Scope**: `tchalanet-server` — authentication, identity bootstrap, request context, RLS context  
> **Audience**: backend developers, reviewers, agents IA, web/mobile integrators  
> **Last reviewed**: 2026-04-28

---

## 1. Purpose

This document defines the canonical authentication and request-context flow for Tchalanet.

It covers:

- Keycloak JWT authentication;
- API scope resolution;
- local `app_user` bootstrap;
- `TchRequestContext` construction;
- super-admin tenant/deleted-visibility overrides;
- RLS context propagation.

It does **not** define fine-grained permissions. See `permission-flow.md`.

---

## 2. High-level pipeline

```text
Client Web / Mobile / POS
  ↓ Authorization: Bearer <jwt>
Spring Security Resource Server
  ↓ validates JWT signature / issuer / audience
UserBootstrapFilter              (NEW — target)
  ↓ guarantees app_user exists + validates status
TchContextFilter                 (existing, to adjust)
  ↓ builds TchRequestContext + MDC + ThreadLocal
Controller / CommandBus / QueryBus
  ↓ business/application flow
RlsAwareDataSource
  ↓ applies PostgreSQL session variables
PostgreSQL RLS policies
```

---

## 3. Current implementation summary

Current code already has:

- `SecurityConfig` as OAuth2 Resource Server;
- JWT audience validation via `app.security.required-audience`;
- Keycloak role extraction from `realm_access.roles` and root `roles`;
- `TchContextFilter` that resolves:
  - `ApiScope`,
  - tenant code / tenant UUID,
  - Keycloak subject,
  - system/custom roles,
  - `appUserId` lookup when already present,
  - deleted visibility,
  - MDC;
- `TchContext` as ThreadLocal;
- `TchContextResolver` helper;
- `@CurrentContext` argument resolver.

Current missing piece:

- there is no runtime `UserBootstrapFilter` yet;
- `TchContextFilter` currently looks up `app_user` but does not create/synchronize it;
- therefore `appUserId` is not guaranteed for authenticated requests.

---

## 4. SecurityConfig responsibility

`SecurityConfig` is responsible for **authentication only**.

It must:

- disable server-side sessions;
- validate JWT issuer and audience;
- convert Keycloak roles into Spring authorities;
- allow public endpoints;
- require authentication for tenant/admin APIs;
- restrict platform APIs to `SUPER_ADMIN` where appropriate.

It must not:

- decide fine-grained business permissions;
- parse tenant payload from request bodies;
- replace `TchRequestContext`;
- perform application-user synchronization.

### 4.1 Coarse-grained authorization only

Spring Security request matchers are allowed only as coarse gates.

Examples:

```java
.requestMatchers("/api/v1/public/**").permitAll()
.requestMatchers("/api/v1/tenant/**").authenticated()
.requestMatchers("/api/v1/admin/**").authenticated()
.requestMatchers("/api/v1/platform/**").hasRole("SUPER_ADMIN")
```

Fine-grained permissions belong to `core.accesscontrol` through method security.

---

## 5. ApiScope

### 5.1 Definition

`ApiScope` represents the **technical execution context** of an HTTP request.

It is derived from the API path only.

```text
PUBLIC    → /api/v1/public/**
TENANT    → /api/v1/tenant/**
ADMIN     → /api/v1/admin/**       (tenant-scoped admin)
PLATFORM  → /api/v1/platform/**
SDR       → /api/v1/_sdr/**
```

`ApiScope` is **not** a user role and **not** a permission.

### 5.2 Purpose

`ApiScope` determines:

- whether a tenant is required;
- whether a default tenant may be used;
- which RLS session variables are applied;
- how observability/audit should categorize the request.

### 5.3 Target adjustment

Current `ApiScopeResolver` maps `/api/v1/admin/**` to `TENANT`.

Target design:

- keep `ADMIN` as a distinct `ApiScope`;
- treat `ADMIN` as tenant-scoped;
- require tenant for `ADMIN` just like `TENANT`;
- keep the distinction for logging, audit, permissions and RLS policy clarity.

---

## 6. UserBootstrapFilter (NEW)

### 6.1 Purpose

`UserBootstrapFilter` guarantees that every authenticated Keycloak user has a local `app_user` representation before `TchContextFilter` builds the request context.

```text
Keycloak JWT sub
  ↓
app_user.keycloak_sub
  ↓
appUserId available in TchRequestContext
```

### 6.2 Position

```text
Spring Security JWT authentication
  ↓
UserBootstrapFilter        @Order(LOWEST_PRECEDENCE - 60)
  ↓
TchContextFilter           @Order(LOWEST_PRECEDENCE - 50)
```

The exact order value may be adjusted, but the rule is strict:

> `UserBootstrapFilter` must run after JWT authentication and before `TchContextFilter`.

### 6.3 Responsibilities

The filter must:

- skip public requests without a JWT;
- read JWT claims:
  - `sub`,
  - `email`,
  - `preferred_username`,
  - `name`,
  - `locale`;
- create `app_user` if missing;
- update identity fields that come from JWT;
- update `last_login_at`;
- validate local user status;
- use `rawDataSource` because `app_user` is global/bootstrap data;
- run in an isolated transaction;
- never execute business logic.

### 6.4 Status behavior

| `app_user.status`  | Behavior                                                    |
| ------------------ | ----------------------------------------------------------- |
| `ACTIVE`           | continue                                                    |
| `PENDING_APPROVAL` | `403 user.pending_approval`                                 |
| `INVITED`          | `403 user.must_complete_profile` or profile-completion flow |
| `SUSPENDED`        | `403 user.suspended`                                        |

### 6.5 Design constraints

`UserBootstrapFilter` must not:

- resolve tenant business data;
- build `TchRequestContext`;
- use RLS-protected repositories;
- call `core.accesscontrol`;
- rely on controller code.

---

## 7. TchContextFilter

### 7.1 Purpose

`TchContextFilter` is the single source of truth for request context initialization.

It builds and publishes `TchRequestContext`.

Current implementation already resolves most context fields and publishes:

- request attribute `REQUEST_CONTEXT`;
- `TchContext` ThreadLocal;
- MDC fields.

### 7.2 Responsibilities

`TchContextFilter` must:

- resolve `ApiScope`;
- extract Keycloak subject from JWT;
- extract tenant code from JWT claim `tenant_code`;
- collect system roles (`TchRole`) and custom roles;
- resolve tenant context via catalog/bootstrap lookup;
- apply super-admin tenant override when allowed;
- resolve deleted visibility when allowed;
- attach `appUserId` guaranteed by `UserBootstrapFilter`;
- set `TchContext`;
- set request attribute `REQUEST_CONTEXT`;
- populate MDC;
- always clear ThreadLocal and MDC in `finally`.

### 7.3 TchRequestContext fields of interest

Current `TchRequestContext` includes:

- `originalTenantCode`;
- `originalTenantUuid`;
- `effectiveTenantCode`;
- `effectiveTenantUuid`;
- `keycloakUserId`;
- `appUserId`;
- `systemRoles`;
- `customRoles`;
- `locale`;
- `requestId`;
- `clientIp`;
- `userAgent`;
- `tenantOverridden`;
- `deletedVisibility`;
- `apiScope`;
- `idempotencyKey`;
- typed `tenantId`;
- `tenantZoneId`;
- `tenantCurrency`.

Current helper methods include:

- `tenantUuid()`;
- `tenantId()`;
- `tenantIdSafe()`;
- `userId()`;
- `userUuid()`;
- `currentUserIdRequired()`;
- `deletedVisibilitySafe()`;
- `isSuperAdmin()`;
- `withTenantContext(...)`;
- `withAppUserId(...)`.

### 7.4 Target adjustment

Once `UserBootstrapFilter` exists:

- `appUserId` should be guaranteed for authenticated non-public requests;
- `currentUserIdRequired()` should represent an exceptional bootstrap failure, not a normal user flow;
- `TchContextFilter` may keep a defensive lookup, but it should treat missing `app_user` as an internal/bootstrap error on protected endpoints.

---

## 8. Tenant resolution

### 8.1 Rules

The client never provides the source-of-truth tenant in body payloads.

Tenant comes from:

1. JWT claim `tenant_code`;
2. default tenant for allowed public endpoints;
3. super-admin override via header.

### 8.2 Tenant required by scope

| Scope      | Tenant required? | Notes                      |
| ---------- | ---------------: | -------------------------- |
| `PUBLIC`   |               no | may use default tenant     |
| `TENANT`   |              yes | user tenant context        |
| `ADMIN`    |              yes | tenant-scoped backoffice   |
| `PLATFORM` |               no | platform/cross-tenant APIs |
| `SDR`      | depends endpoint | internal/admin only        |

If tenant is required and missing/unknown, the request must fail before controller execution.

---

## 9. Super Admin and sensitive overrides

### 9.1 Definition

`isSuperAdmin` means:

```java
ctx.systemRoles().contains(TchRole.SUPER_ADMIN)
```

or:

```java
ctx.isSuperAdmin()
```

It is a user capability, not an API scope.

### 9.2 Sensitive headers

| Header                 | Meaning                              | Allowed for        |
| ---------------------- | ------------------------------------ | ------------------ |
| `X-Tenant-Id`          | override effective tenant            | `SUPER_ADMIN` only |
| `X-Deleted-Visibility` | control soft-deleted rows visibility | `SUPER_ADMIN` only |

`X-Deleted-Visibility` values:

```text
active   → active rows only (default)
deleted  → deleted rows only
all      → active + deleted rows
```

### 9.3 Target rule

If a non-super-admin sends a sensitive override header:

- return `403`;
- do not silently ignore;
- log and audit the attempt.

### 9.4 Header-only recommendation

Target design should prefer headers only:

- `X-Tenant-Id`;
- `X-Deleted-Visibility`.

Avoid query params for these controls because URLs are more likely to leak in logs, browser history, analytics and shared links.

### 9.5 Web and mobile usage

Normal web/mobile apps must not send override headers.

Only platform backoffice UI for `SUPER_ADMIN` may send:

- `X-Tenant-Id` from a controlled tenant selector;
- `X-Deleted-Visibility` from a controlled deleted-visibility selector.

POS/mobile seller flows must never send these headers.

---

## 10. RLS integration

### 10.1 Purpose

RLS is the final tenant-isolation enforcement layer.

Application code sets context. PostgreSQL enforces isolation.

### 10.2 Session variables

`RlsAwareDataSource` must apply session variables before any SQL statement:

```text
app.current_tenant
app.deleted_visibility
app.api_scope
app.is_super_admin
```

Existing implementation may currently set only a subset. Target design is the full set above.

### 10.3 Canonical mapping

| Variable                 | Source                                  |
| ------------------------ | --------------------------------------- |
| `app.current_tenant`     | `ctx.tenantIdSafe()` / effective tenant |
| `app.deleted_visibility` | `ctx.deletedVisibilitySafe()`           |
| `app.api_scope`          | `ctx.apiScope()`                        |
| `app.is_super_admin`     | `ctx.isSuperAdmin()`                    |

### 10.4 Reset rule

Connection cleanup must reset RLS variables before returning the connection to the pool.

At minimum:

```text
app.current_tenant = ''
app.deleted_visibility = 'active'
app.api_scope = 'public'
app.is_super_admin = 'false'
```

---

## 11. Web/mobile client contract

### 11.1 Standard clients

Standard web/mobile/POS requests send:

```http
Authorization: Bearer <jwt>
X-Request-ID: <uuid>              optional
Idempotency-Key: <uuid>           required for critical commands
```

They must not send:

```http
X-Tenant-Id
X-Deleted-Visibility
```

### 11.2 Platform backoffice client

Platform UI for `SUPER_ADMIN` may send:

```http
X-Tenant-Id: <tenantCode|tenantUuid>
X-Deleted-Visibility: active|deleted|all
```

All such requests must be auditable.

---

## 12. Non-HTTP contexts

For startup, batch and async flows:

- bind `TchRequestContext` manually with `TchContext.set(ctx)`;
- clear it in `finally`;
- do not depend on `SecurityContextHolder`.

Current helper:

- `TchContextRunner`;
- `TchRequestContext.startupTenant(...)`.

Target adjustment:

- avoid `ZoneId.systemDefault()` for business semantics;
- prefer tenant timezone or explicit UTC/default app zone.

---

## 13. Absolute rules

- Controllers must not parse JWT.
- Controllers must not resolve tenant UUID.
- Handlers must not use `SecurityContextHolder`.
- Client payloads must not be trusted as tenant source of truth.
- `app_user` bootstrap must bypass RLS but must remain technical, not business logic.
- RLS is mandatory for tenant-scoped tables.

---

## 14. Implementation checklist

- [ ] Introduce `UserBootstrapFilter`.
- [ ] Register it before `TchContextFilter`.
- [ ] Ensure `appUserId` is guaranteed on authenticated protected requests.
- [ ] Introduce `ADMIN` as a distinct `ApiScope`.
- [ ] Enforce `403` for non-super-admin override headers.
- [ ] Propagate `app.api_scope` and `app.is_super_admin` to RLS.
- [ ] Reset all RLS variables on connection close.
- [ ] Document web/mobile header contract.
