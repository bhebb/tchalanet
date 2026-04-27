# Canonical Request Context Filter — TchContextFilter

> **Status**: NORMATIVE  
> **Scope**: tchalanet-server (`common.context`, `common.security`, web, batch)  
> **Audience**: Backend developers, reviewers, ops  
> **Last reviewed**: 2026-01-21  
> **Related**:
>
> - `docs/conventions/api/context.md`
> - `docs/conventions/security_permissions.md`
> - `docs/conventions/rls.md`
> - `docs/conventions/time.md`
> - `docs/conventions/batch.md`

---

## 1. Purpose

`TchContextFilter` is the **single source of truth** for request execution context initialization.

It is responsible for:

- tenant resolution (code → UUID),
- user identification (Keycloak `sub` → `appUserId`),
- system and custom roles,
- soft-delete visibility,
- RLS context initialization,
- request metadata propagation (requestId, MDC, idempotency).

No other layer may replicate these responsibilities.

---

## 2. Order and positioning

- Implemented using `OncePerRequestFilter`
- Annotated with `@Order(Ordered.LOWEST_PRECEDENCE - 50)`
- Executed **after** JWT authentication  
  (`SecurityContextHolder` is already populated)

---

## 3. Responsibilities (NON-DELEGABLE)

The filter:

1. Resolves the `ApiScope`  
   (`PUBLIC`, `TENANT`, `ADMIN`, `PLATFORM`, `SDR`)
2. Builds an immutable `TchRequestContext` containing:

- original and effective tenant codes
- tenant UUID (resolved later)
- `tenantId` (typed), `tenantZoneId`, `tenantCurrency`
- `keycloakUserId` (JWT `sub`)
- `appUserId` (bootstrap DB lookup)
- system roles (`TchRole`) and custom roles
- `requestId`, `clientIp`, `userAgent`, `locale`
- `deletedVisibility`
- `idempotencyKey`

3. Applies the resolution and security rules below.

---

## 4. Tenant rules (V1)

### PUBLIC

- `defaultTenant` allowed via configuration
- tenant **may** be resolved if a code is present
- missing tenant is allowed

### TENANT

- tenant is **required**
- missing tenant → **403**
- unknown tenant → **403**

### ADMIN / PLATFORM

- tenant optional
- resolved if present

---

## 5. SUPER_ADMIN override

A user with role `SUPER_ADMIN` may:

### Tenant override

- via header `X-Tenant-Id`
- or query parameter `?tenantId=`

Effects:

- replaces the effective tenant
- sets `tenantOverridden = true`

### Soft-delete visibility override

- via header `X-Deleted-Visibility`
- allowed values: `active | deleted | all`

---

## 6. Bootstrap tenant & user (bypass RLS)

### Tenant bootstrap

- `TenantBootstrapLookup`:
  - resolves `tenantCode → tenantId`
  - loads `tenantZoneId` and `tenantCurrency`
  - uses a **bypass-RLS datasource**
  - protected by a short TTL, `Clock`-based cache

### User bootstrap

- `UserBootstrapLookup`:
  - resolves `keycloak sub → appUserId`
  - bypasses RLS
  - protected by a short TTL cache

### Rules

- Bootstrap lookups are **not business services**
- They MUST NOT be used in domain or application logic

---

## 7. Context publication

Once built, the context is:

- attached to the HTTP request  
  `request.setAttribute(REQUEST_CONTEXT, ctx)`
- published in a `ThreadLocal`  
  `TchContext.set(ctx)`
- injected into MDC:
  - `tenant_original`
  - `tenant_effective`
  - `tenant_overridden`
  - `tenant_uuid`
  - `tz`
  - `ccy`
  - `kc_user_id`
  - `reqId`
  - `idem`

The context is **always cleared** in a `finally` block.

---

## 8. Absolute rule

❌ No controller, handler, repository, or service may:

- resolve the tenant,
- parse JWTs,
- access `SecurityContext`,
- manipulate MDC,
- call `set_config`,
- inject `tenant_id` from client payloads.

All flows go through **`TchContextFilter` + `TchContext`**.

---

## 9. Persistence & RLS

- `TenantEntityListener` automatically sets `tenant_id`
- Tenant-scoped entities extend `BaseTenantEntity`
- The client **never provides** `tenant_id`

RLS is the **last line of defense**, never the first.

---

## 10. PR checklist — Context

- [ ] `TchContextFilter` is the single initialization point
- [ ] `tenantId`, `tenantZoneId`, `tenantCurrency` populated after resolution
- [ ] SUPER_ADMIN override explicit and auditable
- [ ] No JWT access outside the filter
- [ ] MDC populated correctly
- [ ] Context always cleared
