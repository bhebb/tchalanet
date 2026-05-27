# Tasks

## 1. POS ticket verification

- [ ] Add `POST /tenant/cashier/tickets/verify`.
- [ ] Add `CashierVerifyTicketRequest(scannedValue)` with Jakarta validation.
- [ ] Add a `TicketScanResolver` that accepts:
  - raw public ticket code;
  - full public verification URL;
  - known legacy formats if any.
- [ ] Resolve public code via sales public/secure query, not by exposing internal ticket ids.
- [ ] Query payout claim status by resolved ticket id.
- [ ] Return `CashierTicketVerificationResponse` with status, severity, keys, params, and available actions.
- [ ] Ensure verification never executes payout.

## 2. Contextual message contract

- [ ] Define response fields: `status`, `severity`, `titleKey`, `messageKey`, `params`, `availableActions`.
- [ ] Add POS statuses:
  - `NOT_FOUND`
  - `NOT_PAYABLE_PENDING_DRAW`
  - `NOT_PAYABLE_RESULT_PENDING`
  - `NOT_PAYABLE_LOST`
  - `PAYABLE`
  - `ALREADY_PAID`
  - `BLOCKED`
  - `CANCELLED`
  - `VOIDED`
  - `REPAIR_REQUIRED`
  - `OPERATION_NOT_ALLOWED`
- [ ] Add i18n key names to catalog/i18n seed or documentation.

## 3. Readiness / cashier home

- [ ] Add `GET /tenant/cashier/readiness` or enrich existing cashier home endpoint.
- [ ] Return `ready`, `blockers`, `badges`, `notifications`.
- [ ] Include non-blocking notification if previous draws have payout claims `OPEN` or `BLOCKED`.
- [ ] Do not introduce ack/review workflow in V1.
- [ ] Add action `VIEW_PAYOUTS_TO_PROCESS`.

## 4. Payout action integration

- [ ] Ensure `PAYABLE` response returns an action of type `EXECUTE_PAYOUT`, not a direct payment result.
- [ ] Confirm the actual payment endpoint still dispatches `ExecutePayoutCommand`.
- [ ] Revalidate trusted operational context for payout execution.

## 5. Tests

- [ ] Verify raw public code scan.
- [ ] Verify full URL scan.
- [ ] Verify pending draw/result pending/lost/payable/already paid/blocked statuses.
- [ ] Verify readiness returns no noise when no old unpaid claims exist.
- [ ] Verify readiness returns non-blocking notification when previous unpaid claims exist.
