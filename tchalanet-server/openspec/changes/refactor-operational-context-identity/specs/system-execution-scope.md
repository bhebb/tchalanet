# Spec — SYSTEM execution scope

## Requirement

`SYSTEM` MAY exist as an internal execution scope for scheduler, batch, startup, retry, and outbox flows.

## Rules

- `SYSTEM` is not a public HTTP scope by default.
- `SYSTEM` does not imply cross-tenant access.
- Tenant-scoped system work must bind tenant context explicitly.
- Global system work may access only global/non-RLS tables unless an explicit job policy allows more.
- Audit must record actor type as system and include job/execution id.

## Acceptance criteria

- No controller maps public routes under `/api/v1/system/**` unless ADR-approved.
- Batch jobs create explicit context before DB access.
- Event/listener/retry flows do not assume ambient HTTP ThreadLocal context.
