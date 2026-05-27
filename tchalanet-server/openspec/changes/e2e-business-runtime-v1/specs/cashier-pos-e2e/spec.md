# Spec: Cashier POS E2E

## ADDED Requirements

### Requirement: POS home is compact

`/tenant/cashier/home` SHALL return operational context, session, draw and actions without full PageModel.

### Requirement: POS flow validates sell, print and send

The happy path SHALL sell a ticket, print it and trigger send if configured.

### Requirement: POS blockers are tested

Missing context, locked terminal, blocked outlet, closed session and cross-tenant IDs SHALL be rejected.
