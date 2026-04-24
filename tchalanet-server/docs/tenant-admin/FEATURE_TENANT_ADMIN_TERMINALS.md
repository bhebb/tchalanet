# FEATURE_TENANT_ADMIN_TERMINALS

> **Scope**: Backend (`tchalanet-server`)
> **Feature**: Tenant Admin
> **Slice**: `terminals` > **Module**: `com.tchalanet.server.features.tenantadmin.terminals` > **Status**: **NORMATIVE** > **Style**: Feature / Vertical Slice
> **Owns**: UI orchestration only
> **Owns NO business truth**

---

## 1. Purpose

This slice represents the **Terminal / Device management area**
of the Tenant Admin portal.

It allows a tenant administrator to:

- register terminals
- pair or activate devices
- monitor terminal state
- revoke or reset terminals

---

## 2. Source of truth (OWNING CORE)

### `core.terminal` (or equivalent domain)

Owns:

- terminal lifecycle
- pairing and activation rules
- security credentials
- terminal state machine

---

## 3. Non-duplication rule (CRITICAL)

The `tenantadmin/terminals` slice MUST NOT:

- generate or store device secrets
- validate pairing rules
- mutate terminal state directly
- implement security logic

If a terminal flow is missing:
➡️ it MUST be implemented in `core.terminal`.

---

## 4. Responsibilities of the feature slice

The slice MAY:

- orchestrate terminal registration flows
- call pairing / activation commands
- list terminals and states
- expose setup and monitoring screens

The slice MUST NOT:

- manage cryptographic material
- enforce security invariants

---

## 5. Canonical UI flows

### 5.1 Register / pair terminal

1. Call `core.terminal.RegisterTerminalCommand`
2. Call `core.terminal.PairTerminalCommand`
3. Return `TerminalSetupView`

---

### 5.2 Monitor terminals

1. Call `core.terminal.ListTerminalsQuery`
2. Assemble `TerminalStatusView`

---

## 6. API surface (BFF-style)

> Logical intent only.

- `GET /tenant/admin/terminals`
- `POST /tenant/admin/terminals`
- `POST /tenant/admin/terminals/{terminalId}/pair`
- `DELETE /tenant/admin/terminals/{terminalId}`

---

## 7. Internal structure

The slice follows **Feature Rules (81)**:

- `web` — controllers
- `app` — orchestration services
- `model` — UI models
- `mapper` — mapping logic

**Rule of 3 applies**.

---

## 8. Typed IDs & RLS

- Typed IDs are mandatory
- No UUID leakage
- Tenant isolation enforced by RLS

---

## 9. TL;DR

- Terminal logic lives in `core.terminal`
- Feature only orchestrates and presents
- Security rules never leak into features
