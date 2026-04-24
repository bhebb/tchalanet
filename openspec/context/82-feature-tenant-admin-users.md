# OpenSpec — Feature Tenant Admin Users (82)

> **Scope**: Backend (`tchalanet-server`)
> **Applies to**: `com.tchalanet.server.features.tenantadmin.users.*` > **Status**: **NORMATIVE** > **Purpose**: structural + orchestration rules (**NOT functional**)
> **Style**: Vertical Slice / BFF (orchestration only)

---

## 1. Definition (what this feature slice IS)

The `tenantadmin.users` slice is the **Tenant Admin “Users” UI boundary**. It exists to serve **screen-level** workflows such as:

- manage tenant users (list/details)
- create/update user identity
- assign/unassign users to the current tenant
- expose `/me` and `/bootstrap` payloads for the admin UI startup

This slice is **not a domain** and owns **no business truth**.

---

## 2. Source of truth (OWNING CORES)

### 2.1 `core.user` (identity + preferences)

Owns:

- global user identity lifecycle (CRUD)
- user preferences (UI preferences)

Does NOT own:

- tenant membership
- tenant-scoped lists

---

### 2.2 `core.tenantuser` (membership + current user + bootstrap)

Owns:

- user ↔ tenant assignment/unassignment
- list users of a tenant
- `/me` (current user projection)
- `/bootstrap` (startup projection)

---

### 2.3 Optional: `core.accesscontrol` (roles/perms)

If tenant admin UI requires roles/permissions:

- the source of truth is `core.accesscontrol`
- the feature may orchestrate assignment + read models
- the feature must not decide authorization semantics

---

## 3. Non-duplication policy (CRITICAL)

This slice MUST NOT:

- implement identity rules (belongs to `core.user`)
- implement membership rules (belongs to `core.tenantuser`)
- store preferences
- define validation rules beyond request-shape validation
- access persistence directly (repositories, entities, EntityManager)

If a required UI use-case is missing: ➡️ it MUST be implemented as a handler in the owning core, then orchestrated here.

---

## 4. Allowed responsibilities (orchestration)

The slice MAY:

- call multiple core commands/queries (sequential orchestration)
- aggregate identity + membership + roles into **UI models**
- expose BFF endpoints aligned with screens (wizard steps, list filters, details panels)
- enrich payloads with **UI-only** composition (not business truth)

The slice MUST NOT:

- bypass core transaction boundaries
- manually mutate domain state
- enforce durable invariants

---

## 5. Mandatory routing intent (BFF surface)

> Exact prefixes depend on global routing conventions. These are logical endpoints.

### 5.1 Tenant-scoped users screens

- List tenant users: `GET /tenant/admin/users`
- Tenant user details (screen payload): `GET /tenant/admin/users/{userId}`
- Create user (identity) for tenant flows: `POST /tenant/admin/users`
- Update user identity: `PUT /tenant/admin/users/{userId}`
- Update user preferences: `PATCH /tenant/admin/users/{userId}/preferences`

### 5.2 Membership commands

- Assign user to tenant: `POST /tenant/admin/tenant-users`
- Unassign user from tenant: `DELETE /tenant/admin/tenant-users/{tenantUserId}`
  - (alternative shape allowed: `DELETE /tenant/admin/tenants/{tenantId}/users/{userId}`)

### 5.3 Startup payloads (core-owned)

- Current user: `GET /tenant/admin/me` (source of truth: `core.tenantuser`)
- Bootstrap: `GET /tenant/admin/bootstrap` (source of truth: `core.tenantuser`)

Rules:

- `/me` and `/bootstrap` MUST remain **core-owned** in meaning.
- Feature MAY extend them with extra composed data, but MUST NOT redefine semantics.

---

## 6. Models (UI contracts) — `model/`

Naming:

- Inputs: `XxxRequest`
- Outputs: `XxxResponse`
- Screen read models: `XxxView`, `XxxItem`, `XxxSummary`

The term **DTO** is forbidden in this slice.

---

## 7. Internal structure (Rule of 3)

The slice uses role packages only when they contain **≥ 3 elements**:

- `web/` — controllers
- `app/` — orchestration services
- `model/` — UI contracts
- `mapper/` — mapping/assembly

If a role has `< 3 classes`, keep it flat at slice root.

---

## 8. Typed IDs & tenant scoping

- Typed IDs are mandatory in models, controllers, and services.
- UUID usage is forbidden in `features.*` packages.
- Tenant resolution relies on request context; feature must not compute tenant filters manually.

---

## 9. Acceptance checklist (must pass)

- [ ] No repositories/entities/JPA imports in this slice
- [ ] All mutations go through core command handlers
- [ ] All screen payloads are `model/*` UI contracts
- [ ] `/me` and `/bootstrap` semantics remain core-owned
- [ ] Rule of 3 respected for packages
