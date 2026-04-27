# FEATURE_TENANT_ADMIN_DRAWS

> **Scope**: Backend (`tchalanet-server`)
> **Feature**: Tenant Admin
> **Slice**: `draws` > **Module**: `com.tchalanet.server.features.tenantadmin.draws` > **Status**: **NORMATIVE** > **Style**: Feature / Vertical Slice
> **Owns**: UI orchestration only
> **Owns NO business truth**

---

## 1. Purpose

This slice represents the **Draw Configuration area** of the Tenant Admin portal. It allows a tenant administrator to:

- configure tenant draw channels
- activate or deactivate draws
- manage draw windows and schedules
- view current and upcoming draws

---

## 2. Source of truth (OWNING CORE)

### `core.draw`

Owns:

- tenant draw channels
- draw lifecycle and activation rules
- draw windows and schedules
- draw-related invariants

---

## 3. Non-duplication rule (CRITICAL)

The `tenantadmin/draws` slice MUST NOT:

- compute draw eligibility
- decide draw outcomes
- manage result ingestion
- bypass draw lifecycle rules

If a draw-related use-case is missing: ➡️ it MUST be implemented in `core.draw`.

---

## 4. Responsibilities of the feature slice

The slice MAY:

- orchestrate draw configuration commands
- list draw channels and windows
- assemble draw dashboards
- expose multi-step configuration flows

The slice MUST NOT:

- enforce scheduling rules
- mutate draw state outside core commands

---

## 5. Canonical UI flows

### 5.1 Configure tenant draw

1. Call `core.draw.ConfigureDrawChannelCommand`
2. Call `core.draw.ActivateDrawChannelCommand`
3. Return `TenantDrawView`

---

### 5.2 List draws

1. Call `core.draw.ListTenantDrawsQuery`
2. Assemble `TenantDrawListView`

---

## 6. API surface (BFF-style)

> Logical intent only.

- `GET /tenant/admin/draws`
- `POST /tenant/admin/draws`
- `PUT /tenant/admin/draws/{drawId}`
- `POST /tenant/admin/draws/{drawId}/activate`
- `POST /tenant/admin/draws/{drawId}/deactivate`

---

## 7. Internal structure

The slice follows **Feature Rules (81)**:

- `web` — controllers
- `app` — orchestration services
- `model` — UI models
- `mapper` — assembly logic

**Rule of 3 applies**.

---

## 8. Typed IDs & RLS

- Typed IDs are mandatory
- No UUID leakage
- Tenant isolation via RLS and request context

---

## 9. TL;DR

- Draw rules live in `core.draw`
- Feature orchestrates configuration UI
- No lifecycle or scheduling logic in feature
