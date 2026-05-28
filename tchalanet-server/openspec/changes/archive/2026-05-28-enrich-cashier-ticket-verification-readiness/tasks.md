# Tasks

## 1. POS ticket verification

- [x] Add `POST /tenant/cashier/tickets/verify`.
- [x] Add `CashierVerifyTicketRequest(scannedValue)` with Jakarta validation.
- [x] Add a `TicketScanResolver` that accepts:
  - raw public ticket code;
  - full public verification URL;
  - known legacy formats if any.
- [x] Resolve public code via sales public/secure query, not by exposing internal ticket ids.
- [x] Query payout claim status by resolved ticket id.
- [x] Return `CashierTicketVerificationResponse` with status, severity, keys, params, and available actions.
- [x] Ensure verification never executes payout.

## 2. Contextual message contract

- [x] Define response fields: `status`, `severity`, `titleKey`, `messageKey`, `params`, `availableActions`.
- [x] Add POS statuses:
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
- [x] Add i18n key names to catalog/i18n seed or documentation.

## 3. Readiness / cashier home

- [x] Add `GET /tenant/cashier/readiness` or enrich existing cashier home endpoint.
- [x] Return `ready`, `blockers`, `badges`, `notifications`.
- [x] Include non-blocking notification if previous draws have payout claims `OPEN` or `BLOCKED`.
- [x] Do not introduce ack/review workflow in V1.
- [x] Add action `VIEW_PAYOUTS_TO_PROCESS`.

## 4. Payout action integration

- [x] Ensure `PAYABLE` response returns an action of type `EXECUTE_PAYOUT`, not a direct payment result.
- [x] Confirm the actual payment endpoint still dispatches `ExecutePayoutCommand`.
- [x] Revalidate trusted operational context for payout execution.

## 5. Tests

- [x] Verify raw public code scan.
- [x] Verify full URL scan.
- [x] Verify pending draw/result pending/lost/payable/already paid/blocked statuses.
- [x] Verify readiness returns no noise when no old unpaid claims exist.
- [x] Verify readiness returns non-blocking notification when previous unpaid claims exist (OPEN and BLOCKED).
