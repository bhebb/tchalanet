# Autonomy Domain — Documentation (V1)

## 1. Purpose

The **Autonomy** domain determines **how much freedom an actor has to proceed when an operation is blocked by business rules** (typically limits).

> **Autonomy does NOT decide if an operation is allowed.**  
> It decides **whether a blocked operation requires human approval, and at which role level**.

In short:

- **LimitPolicy** answers: _“Is this operation allowed, warned, or blocked?”_
- **Autonomy** answers: _“If blocked, can it proceed with approval, and who can approve?”_

---

## 2. What Autonomy Is / Is Not

### Autonomy IS

- A **runtime decision helper** used by `sales` and `payout`
- A **hierarchical policy system**
- A way to express **organizational trust and delegation**

### Autonomy IS NOT

- ❌ A permission or RBAC system
- ❌ A financial or limit rule engine
- ❌ UI logic
- ❌ A replacement for LimitPolicy

---

## 3. Core Concept — Autonomy Policy Rule

An **AutonomyPolicyRule** defines the autonomy level for **one specific target**.

A rule answers:

> _“If an operation is blocked for this target, does it require approval, and by whom?”_

---

## 4. Targets (Scope of a Rule)

Each rule applies to exactly one target.

| TargetType | Meaning                             |
| ---------- | ----------------------------------- |
| TENANT     | Global policy for the entire tenant |
| OUTLET     | Policy for a specific point of sale |
| TERMINAL   | Policy for a specific terminal      |
| AGENT      | Policy for a specific agent         |

---

## 5. Fields of an Autonomy Policy Rule

> ⚠️ `tenant_id` is **not part of the domain model**.  
> It is enforced by the database via **RLS (Row-Level Security)**.

| Field                  | Description                                            |
| ---------------------- | ------------------------------------------------------ |
| targetType             | Scope of the rule (TENANT / OUTLET / TERMINAL / AGENT) |
| targetId               | Identifier of the target                               |
| level                  | Autonomy level                                         |
| requireApprovalOnBlock | Whether approval is required when blocked              |
| approvalRole           | Role required to approve                               |
| enabled                | Whether the rule is active                             |
| startsAt / endsAt      | Optional time window                                   |
| version                | Optimistic locking                                     |
| deletedAt (infra)      | Soft delete (audit/history)                            |

---

## 6. Autonomy Levels

### NONE

- No autonomy
- Every blocked operation requires approval
- Most restrictive

### PARTIAL (system default)

- Partial autonomy
- Some operations may require approval
- Default when no rule exists

### FULL

- Full autonomy
- Blocked operations do **not** trigger approval
- Least restrictive

---

## 7. Approval Roles

Defines **who is allowed to approve** when approval is required.

| Role     | Meaning                          |
| -------- | -------------------------------- |
| OPERATOR | Standard operational approval    |
| ADMIN    | Elevated administrative approval |

> `approvalRole` is relevant only when `requireApprovalOnBlock = true`.

---

## 8. Hierarchical Resolution (Most Specific Wins)

When resolving autonomy, rules are evaluated in this order:

AGENT
↓
TERMINAL
↓
OUTLET
↓
TENANT
↓
DEFAULT

---

The **first active rule** (enabled + valid time window) is applied.

---

## 9. Default Policy (Fallback)

If **no rule exists at any level**, the system applies a default policy:

- `level = PARTIAL`
- `requireApprovalOnBlock = true`
- `approvalRole = OPERATOR`

This guarantees predictable behavior even with no configuration.

---

## 10. Runtime Result — ResolvedAutonomy

The resolution produces a **ResolvedAutonomy** object:

| Field                  | Meaning                             |
| ---------------------- | ----------------------------------- |
| level                  | Effective autonomy level            |
| requireApprovalOnBlock | Whether approval is required        |
| approvalRole           | Required role for approval          |
| source                 | Target level that provided the rule |

This result is **used only at runtime** (sales / payout).

---

## 11. Integration with LimitPolicy

### Runtime Flow

1. **LimitPolicy** evaluates the operation  
   → `ALLOW` / `WARN` / `BLOCK`

2. If the outcome is `BLOCK`:

- **Autonomy is resolved**
- Approval requirements are determined

### Decision Matrix

| Limit Outcome          | Autonomy | Result                       |
| ---------------------- | -------- | ---------------------------- |
| ALLOW                  | –        | Transaction proceeds         |
| WARN                   | –        | Proceeds with warning        |
| BLOCK + approval=false | –        | Transaction rejected         |
| BLOCK + approval=true  | role     | Transaction pending approval |

Autonomy is **never consulted** if the limit is not blocked.

---

## 12. Read vs Runtime Semantics

### Admin / Ops Read

- Used for configuration and overview
- Respects `deleted_visibility`
- Super Admin may view deleted rules

### Runtime Resolve

- Ignores deleted rules
- Ignores disabled rules
- Ignores rules outside their time window
- Always operates in safe mode

---

## 13. Persistence & RLS Rules

- `tenant_id` is enforced by PostgreSQL RLS
- No `tenantId` is accepted from the client
- Super Admin may override `deleted_visibility`
- Runtime always operates with `deleted_visibility = active`

---

## 14. API Surfaces

### Core Autonomy

- Exposes **commands and queries**
- Controllers are **internal / ops only**
- Not a public UI contract

### Feature Tenant Admin

- Exposes `/tenant/admin/**`
- Calls core handlers
- Returns UI-friendly overview payloads

---

## 15. Mental Model (TL;DR)

> **LimitPolicy says:** “This is too risky.”  
> **Autonomy says:** “Can someone decide anyway?”

Autonomy exists **only** to define **who is trusted to override a block** — at the agent, terminal, outlet, or tenant level.
