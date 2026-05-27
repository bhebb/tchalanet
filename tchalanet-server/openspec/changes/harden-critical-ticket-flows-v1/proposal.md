# Change: Harden Critical Ticket Flows V1

## Status

`draft`

## Priority

`P0`

## Scope

- `core.sales`
- `core.promotion` integration in sales
- `features.cashier`
- `features.ticketverify`
- `platform.document` integration
- `platform.communication` integration

## Why

The ticket sale, receipt/print, send receipt, and public ticket verification flows are the most critical endpoints of Tchalanet. They form the commercial/customer proof of sale and will be used by sellers, tenants, customers, and support teams.

These flows must be coherent and canonical:

- `sell` materializes ticket truth.
- `print/receipt` renders the official proof of sale.
- `ticketverify` exposes a safe public proof.
- promotions such as Maryaj gratuit, odds boost, and waived charges must be snapshotted and displayed consistently.
- features must not recompute money, winnings, promotions, or customer statuses.

## Goals

- Make `core.sales` the owner of canonical ticket evidence.
- Move receipt business layout decisions out of `features.cashier` and `TicketPrintDocumentMapper`.
- Ensure promotion effects are materialized in sales and exposed through receipts, events, and public verification.
- Ensure limit/exposure checks consider final promotion-adjusted sale risk.
- Harden POS print/send operational-context validation.
- Harden public ticket verification with no-store/noindex, rate-limit, and safe status exposure.
- Ensure side effects are after-commit and context-explicit.

## Non-goals

- Building a full promotion rules engine.
- Adding a distributed outbox in this change.
- Implementing Redis-backed rate limiting immediately; document it as production follow-up if multi-instance.
- Changing public verification into an authenticated flow.
- Moving `platform.document` rendering logic into sales.

## Design summary

```text
core.sales
  TicketPrintView
    -> TicketReceiptAssembler
    -> TicketReceiptPrintFormatter
    -> TicketReceiptMessageFormatter
    -> TicketBackupAssembler

features.cashier
  validates operational context
  chooses format/channel
  asks core.sales for canonical receipt/message content
  maps content to DocumentRenderRequest
  delegates rendering to platform.document
  delegates delivery to platform.communication

features.ticketverify
  public BFF only
  normalizes input
  rate-limits
  asks core.sales VerifyTicketByPublicCodeQuery
  maps safe public view
```

## Key decisions

1. `core.sales` owns canonical ticket evidence.
2. `TicketPrintDocumentMapper` is technical only: `TicketReceiptPrintContent -> DocumentRenderRequest`.
3. Receipt, backup, print, message, and public verification must share the same sale snapshots where applicable.
4. Promotions are materialized at sell time and never recalculated by print or verify.
5. Limit/exposure checks use final promotion-adjusted lines/money when promotions increase risk.
6. `TicketPlacedEvent` includes promotion snapshots per line.
7. Communication after commit receives tenant/user/correlation explicitly and does not depend on implicit `TchContext`.
8. Public verify is read-only and sales-owned for visibility/status/truth.

## Risks

- Existing print output can change. Add golden tests for representative receipts.
- Event schema changes may require consumers to be updated.
- Verification projection may require SQL/view changes.
- Promotion snapshots may require migration and backfill strategy for existing dev data.

## Rollout

Recommended implementation order:

1. Enrich sales line/promotion snapshots.
2. Fix `SalePreparationOrchestrator` final exposure input.
3. Enrich `TicketPlacedEvent`.
4. Build rich `TicketReceiptPrintContent` in `core.sales`.
5. Move receipt layout logic from `TicketPrintDocumentMapper` to sales formatter.
6. Simplify `TicketPrintDocumentMapper`.
7. Fix print/send operational context validation.
8. Enrich `RecordTicketPrintCommand`.
9. Enrich `TicketVerificationProjection/View`.
10. Harden public verify controller/rate-limit.
11. Add integration/E2E tests: sell -> print -> verify.
