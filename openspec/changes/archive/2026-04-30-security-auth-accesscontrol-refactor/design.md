# Design — Security Auth & Access Control Refactor

## 1. Target authentication pipeline

```text
Client
  ↓ Bearer JWT
Spring Security Resource Server
  ↓ validates token
UserBootstrapFilter              NEW
  ↓ guarantees app_user + status
TchContextFilter                 EXISTING, adjusted
  ↓ builds TchRequestContext
Controller / method security
  ↓
RLS-aware persistence
```

---

## 2. UserBootstrapFilter

### Purpose

Guarantee `app_user` exists for every authenticated protected request.

### Inputs

JWT claims:

- `sub`;
- `email`;
- `preferred_username`;
- `name`;
- `locale`.

### Behavior

- Skip public requests without JWT.
- Upsert `app_user` through `rawDataSource`.
- Update safe identity fields and `last_login_at`.
- Validate status.
- Stop request with 403 if status blocks access.

### Ordering

Must run after Spring JWT authentication and before `TchContextFilter`.

---

## 3. TchContextFilter adjustments

`TchContextFilter` remains the source of truth for request context.

It should:

- resolve `ApiScope`;
- resolve tenant;
- resolve system/custom roles;
- apply super-admin override;
- apply deleted visibility;
- attach guaranteed `appUserId`;
- publish request attribute + ThreadLocal + MDC.

Target adjustment:

- introduce distinct `ADMIN` scope for `/api/v1/admin/**`;
- treat missing `app_user` after bootstrap as an exceptional failure on protected endpoints.

---

## 4. ApiScope and super-admin overrides

`ApiScope` is request context, not authorization.

Target values:

```text
PUBLIC
TENANT
ADMIN
PLATFORM
SDR
```

Sensitive headers:

```text
X-Tenant-Id
X-Deleted-Visibility
```

Rules:

- allowed only for `SUPER_ADMIN`;
- non-super-admin usage returns 403;
- headers are preferred over query params;
- usage must be auditable.

---

## 5. RLS context

RLS session variables target set:

```text
app.current_tenant
app.deleted_visibility
app.api_scope
app.is_super_admin
```

They are derived from `TchRequestContext`.

The connection wrapper must reset all variables before returning a connection to the pool.

---

## 6. Permission evaluation target

Canonical flow:

```text
@PreAuthorize("hasPermission('permission.key')")
  ↓
TchPermissionEvaluator
  ↓
TchContextResolver.currentOrThrow()
  ↓
QueryBus.send(CheckUserPermissionsQuery)
  ↓
core.accesscontrol
```

### TchPermissionEvaluator changes

Must:

- use `TchContextResolver`, not `Authentication.getPrincipal()`;
- call `QueryBus`, not handler directly;
- catch domain deny exceptions and return `false`;
- never call repositories.

### RequiresPermissionAspect

Deprecated or removed.

No new controller should use it.

---

## 7. SecurityConfig target

SecurityConfig remains a coarse gate.

Target style:

```java
.requestMatchers("/api/v1/public/**").permitAll()
.requestMatchers("/api/v1/tenant/**").authenticated()
.requestMatchers("/api/v1/admin/**").authenticated()
.requestMatchers("/api/v1/platform/**").hasRole("SUPER_ADMIN")
```

Fine-grained permissions belong to method security and `core.accesscontrol`.

---

## 8. Documentation deliverables

Create/update:

```text
tchalanet-docs/docs/01-architecture/flows/authentication-flow.md
tchalanet-docs/docs/01-architecture/flows/permission-flow.md
openspec/context/90-security-flows-guide.md
```
