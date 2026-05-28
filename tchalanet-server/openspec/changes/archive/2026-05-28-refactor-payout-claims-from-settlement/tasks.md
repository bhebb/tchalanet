# Tasks

## 1. Domain language and statuses

- [ ] Introduce or rename domain model to `PayoutClaim`.
- [ ] Replace normal workflow statuses with:
  - `OPEN`
  - `BLOCKED`
  - `PAID`
  - `CANCELLED`
  - `REVERSED`
- [ ] Add `PayoutClaimSource`:
  - `SALES_SETTLEMENT`
  - `OPS_RECONCILIATION`
  - `MANUAL_ADMIN_CORRECTION` if needed for controlled repair only.

## 2. Persistence migration

- [ ] Keep table name `payout` for V1 unless a larger rename is approved.
- [ ] Add columns:
  - `draw_id`
  - `source_event_id`
  - `source`
  - `opened_at`
  - `blocked_by`
  - `blocked_at`
  - `block_reason`
  - `reversed_by`
  - `reversed_at`
  - `reverse_reason`
- [ ] Backfill `opened_at` from `requested_at` or `created_at`.
- [ ] Add unique protection by `(tenant_id, ticket_id)` or `(tenant_id, ticket_id, settlement_version)` if settlement version exists.
- [ ] Add indexes by status/opened_at, ticket, draw/status.

## 3. Claim opening from settlement

- [ ] Add `OpenPayoutClaimFromSettlementCommand`.
- [ ] Add command handler with idempotence.
- [ ] Listen to `TicketWinningSettlementCreatedEvent` using `@TransactionalEventListener(AFTER_COMMIT)`.
- [ ] Create claim with source `SALES_SETTLEMENT`, status `OPEN`, source event id.
- [ ] Publish `PayoutClaimOpenedEvent` after commit.

## 4. Execute payout hardening

- [ ] Use `writer.lockByIdForPayment(payoutId)` / `FOR UPDATE` equivalent.
- [ ] Revalidate trusted operational context in transaction.
- [ ] Recheck terminal locked, outlet payout blocked, and session open.
- [ ] Verify status is `OPEN` before payment.
- [ ] Verify payout amount against Sales settled payout snapshot.
- [ ] Publish `PayoutPaidEvent` after commit.

## 5. Manual actions

- [ ] Add `BlockPayoutClaimCommand`.
- [ ] Add `UnblockPayoutClaimCommand`.
- [ ] Add `CancelPayoutClaimCommand` for unpaid claims.
- [ ] Add `ReversePayoutPaymentCommand` for paid claims.
- [ ] Ensure all admin write endpoints are audited and permission-protected.

## 6. Queries

- [ ] Add find claim by ticket/public-code support for Cashier composition.
- [ ] Add list claims by status/date/draw/outlet/session.
- [ ] Add list claims to process for POS.

## 7. Tests

- [ ] Winning settlement creates claim once.
- [ ] Duplicate event does not create duplicate claim.
- [ ] Two concurrent payout executions produce only one payment.
- [ ] Blocked/cancelled/reversed claims cannot be paid.
- [ ] Amount mismatch blocks payout.
- [ ] Payout paid event updates Sales projection through listener only.
