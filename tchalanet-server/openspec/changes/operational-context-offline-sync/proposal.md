# Change: operational-context-offline-sync

## Summary

Unify how Tchalanet resolves, carries, trusts, validates and consumes operational context (terminal / outlet / sales session) for POS, cashier, operator and offline-sync workflows, and lock down the boundary between offline-sync technical truth and sales official truth.

## Motivation

Today the backend exposes **two canonical producers** of HTTP request context:

- `TchContextFilter` (Order -50) resolves tenant + actor.
- `OperationalContextFilter` (Order -45) re-binds the context with operational fields.

This dual-producer design creates ordering and ownership ambiguity, and the filter calls `queryBus.ask(GetCurrentOperationalContextQuery)` against a handler that does **not exist**. In parallel:

- `OperationalRequestContext` still carries a redundant `selectedByAdmin` boolean alongside `source`.
- `OperationalContextSource` has no `TrustLevel` — sensitive handlers cannot uniformly require a `STRONG` source.
- `SalesSession` aggregate lacks `finalizedAt`/`finalizedBy`, `Outlet` aggregate lacks `payoutBlocked`/`offlineSalesBlocked` (snapshot-only).
- `core.offlinesync` has no domain events; `ApproveOfflineSubmissionCommandHandler` is a TODO stub.
- `core.payout` has no first-class `PosPayoutOperationValidator`; `core.sales` lacks `PosCancelOperationValidator` and `OfflineSaleAcceptanceValidator`.

The intended outcome is to (a) make `TchContextFilter` the single canonical context producer, (b) introduce explicit trust modeling so sensitive handlers can require a `STRONG` source, (c) close the gaps in aggregates, events and validators, and (d) enforce the invariant **OfflineSaleSubmission ≠ Ticket** with explicit rules for `FINALIZED` sessions, draw-result-known and device-time.

## Core decisions

```text
Global context proves tenant/user/scope.
Operational context carries terminal/outlet/session candidates.
Sensitive operations validate operational invariants at action time.
```

- **One canonical context producer**: `TchContextFilter -> contextFactory -> tenantContextResolver -> actorContextResolver -> operationalContextResolver -> contextBinder.bind(finalCtx)`.
- **Remove** `OperationalContextFilter` as a second canonical producer.
- `OperationalContextSource` carries a `TrustLevel { NONE, WEAK, STRONG }`. Sensitive operations require `STRONG`.
- Drop `selectedByAdmin` from `OperationalRequestContext` — `source == ADMIN_SELECTION` already expresses it.
- `core.terminal` owns operational context resolution (device binding → terminal → outlet → candidate session).
- Atomic validations live in their domain owner (`core.terminal`, `core.outlet`, `core.session`); use-case validators (Sales, Payout, Offlinesync) compose them.
- `core.offlinesync` owns technical truth (grants, batches, reservations, submissions, payload hashes, signatures, technical reject reasons); `core.sales` owns official truth (tickets, ticket lines, official sales events).
- A `FINALIZED` session never auto-accepts an offline submission — it must go to `SALES_REVIEW_REQUIRED` with risk flag `FINALIZED_SESSION`.

## Affected areas

- `common.context`
- `common.security`
- `core.terminal`
- `core.outlet`
- `core.session`
- `core.sales`
- `core.payout`
- `core.offlinesync`

## Out of scope

- L1/L2 cache for operational validation (v2 — see `design.md` §cache policy).
- Dedicated quarantine table for offline submissions (v1 stays inline with `SALES_REVIEW_REQUIRED` + risk flags).
- Cross-tenant or cross-terminal context cloning.

## Risks

- **Concurrency**: between validate (read snapshot) and commit, a terminal can be locked, an outlet can close day, a session can be finalized. Mitigation: in-tx minimal re-check inside critical handlers, plus optimistic locking on the aggregates already supporting it. Documented in `design.md` §11.
- **Migration**: existing pre-go-live Flyway scripts must be edited (per repo policy: modify existing `V*.sql` before go-live) to add `sales_session.finalized_at/by` and `outlet.payout_blocked*/offline_sales_blocked*`.
- **Caller break**: removing `OperationalContextFilter` means every test that asserted `Order(-45)` registration must be deleted/updated.

## Acceptance

Implementation is accepted when:

```text
TchContextFilter calls OperationalContextResolver
OperationalRequestContext has no selectedByAdmin
OperationalContextSource has TrustLevel
trustedOperationalContextRequired exists
terminal/outlet/session validations are via QueryBus
use case validators call atomic validations
offline finalized session goes to review
critical handlers document race/concurrency strategy
```
