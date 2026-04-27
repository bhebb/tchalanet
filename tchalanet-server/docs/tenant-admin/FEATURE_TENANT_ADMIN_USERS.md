# FEATURE_TENANT_ADMIN_USERS

> **Scope**: Backend (`tchalanet-server`)
> **Feature**: Tenant Admin
> **Slice**: `users` > **Module**: `com.tchalanet.server.features.tenantadmin.users` > **Status**: **NORMATIVE** > **Style**: Feature / Vertical Slice
> **Owns**: orchestration + UI composition
> **Owns NO business truth**

---

## 1. Purpose

This slice represents the **User Management area** of the Tenant Admin portal. It exists to allow a tenant administrator to:

- manage user identities
- assign and unassign users to the tenant
- manage user preferences
- retrieve current user and bootstrap information for the UI

This slice is **UI-driven** and **orchestration-only**.

---

## 2. Source of truth (OWNING CORES)

### 2.1 `core.user`

Owns:

- global user identity
- CRUD lifecycle of users
- user preferences (UI preferences)

Does NOT own:

- tenant membership
- tenant-specific roles

---

### 2.2 `core.tenantuser`

Owns:

- user ↔ tenant assignment
- listing users of a tenant
- current user projection (`/me`)
- bootstrap projection (`/bootstrap`)

---

### 2.3 Optional related cores

- `core.accesscontrol` (roles / permissions)
- other cores if required by future UI screens

---

## 3. Non-duplication rule (CRITICAL)

The `tenantadmin/users` slice MUST NOT:

- implement user or membership logic
- persist user data
- store preferences
- redefine `/me` or `/bootstrap`
- bypass core handlers

If a required use-case does not exist: ➡️ **add a handler to the owning core**, not to this feature.

---

## 4. Responsibilities of the feature slice

The slice MAY:

- call `core.user` command/query handlers
- call `core.tenantuser` command/query handlers
- orchestrate identity + membership flows
- compose UI-friendly screen models
- expose BFF endpoints aligned with admin screens

The slice MUST NOT:

- enforce invariants
- manage transactions
- write to the database

---

## 5. Canonical UI flows (orchestration examples)

### 5.1 Create user and assign to tenant

1. Call `core.user.CreateUserCommand`
2. Call `core.tenantuser.AssignUserToTenantCommand`
3. Optionally call `core.accesscontrol.AssignRoleCommand`
4. Return a composed `TenantUserView`

---

### 5.2 List users of a tenant

1. Call `core.tenantuser.ListTenantUsersQuery`
2. Optionally enrich with `core.user.GetUsersByIdsQuery`
3. Assemble `TenantUserListView`

---

### 5.3 Current user (`/me`)

- Delegated directly to `core.tenantuser`
- Feature MAY enrich response with UI-only data

---

### 5.4 Bootstrap (`/bootstrap`)

- Delegated to `core.tenantuser`
- Feature MAY extend payload with:
  - feature availability
  - UI defaults
  - lightweight tenant metadata

---

## 6. API surface (BFF-style)

> Exact routes depend on global routing rules. Logical intent only.

- `GET /tenant/admin/users`
- `POST /tenant/admin/users`
- `PUT /tenant/admin/users/{userId}`
- `PATCH /tenant/admin/users/{userId}/preferences`

- `POST /tenant/admin/tenant-users` (assign)
- `DELETE /tenant/admin/tenant-users/{tenantUserId}` (unassign)

- `GET /tenant/admin/me`
- `GET /tenant/admin/bootstrap`

---

## 7. Internal structure

The slice follows **Feature Rules (81)**:

- `web` — controllers
- `app` — orchestration services
- `model` — UI models
- `mapper` — mapping / assembly

**Rule of 3 applies** for package creation.

---

## 8. Typed IDs & RLS

- Typed IDs are mandatory
- No UUID leakage
- Tenant scoping relies on request context + RLS
- No tenant filtering in Java code

---

## 9. TL;DR

- Users logic lives in cores
- This slice orchestrates and composes
- `/me` and `/bootstrap` remain core-owned
- Zero duplication is enforced
