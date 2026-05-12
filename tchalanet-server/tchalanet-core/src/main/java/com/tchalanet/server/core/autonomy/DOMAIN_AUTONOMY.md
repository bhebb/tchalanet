# Core – Autonomy (V1)

## Vision

Autonomy defines override and approval behavior for blocked operations.

It DOES NOT:

- evaluate limits
- execute workflows
- manage permissions
- own transaction state

---

## Runtime model

LimitPolicy
↓
Autonomy
↓
Sales/Payout workflow

---

## Responsibilities

- Resolve autonomy policies
- Determine approval requirements
- Determine escalation role

---

## Targets

- TENANT
- OUTLET
- TERMINAL
- AGENT

Resolution:
AGENT
↓
TERMINAL
↓
OUTLET
↓
TENANT
↓
DEFAULT

Most specific active rule wins.

---

## Default policy

level = PARTIAL
requireApprovalOnBlock = true
approvalRole = OPERATOR

---

## Levels

NONE

- blocked operations require approval

PARTIAL

- standard approval behavior

FULL

- blocked operations are auto-approved

FULL does NOT bypass LimitPolicy.

---

## Runtime objects

ResolvedAutonomy
OverrideDecision

---

## OverrideDecision

```java
public record OverrideDecision(
    boolean allowed,
    boolean requiresApproval,
    ApprovalRole requiredRole,
    OverrideMode mode,
    AutonomySource source
) {}
```

---

## Override modes

- REJECT
- REQUIRE_APPROVAL
- AUTO_APPROVE

---

## Runtime semantics

Autonomy only executes on BLOCK results.

LimitPolicy remains authoritative.
