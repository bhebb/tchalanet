# Tasks

## 1. Domain model

- [ ] Keep module `core.session`.
- [ ] Name aggregate/classes `SalesSession`.
- [ ] Fields:
  - SessionId
  - TenantId
  - UserId seller
  - OutletId
  - TerminalId nullable or required by open command
  - status OPEN/CLOSED/RECONCILED
  - source MANUAL/SCHEDULER/OPS
  - openedAt/closedAt
  - openingFloatCents
  - closingAmountCents
- [ ] Invariants:
  - one OPEN session per seller per tenant
  - closed session cannot be used for new sale
  - tickets remain attached after closure
  - payout can reference selling session even closed

## 2. Commands

- [ ] `OpenSalesSessionCommand`
- [ ] `CloseSalesSessionCommand`
- [ ] `AutoOpenSalesSessionsCommand`
- [ ] `AutoCloseSalesSessionsCommand`
- [ ] `RecomputeSalesSessionTotalsCommand` restricted admin/ops
- [ ] `ReconcileSalesSessionCommand` optional/post-MVP

## 3. Queries / read endpoints

Do not rely on overview.

- [ ] `GetSalesSessionByIdQuery`
- [ ] `GetCurrentSalesSessionQuery(tenantId, userId)`
- [ ] `ListSalesSessionsQuery`
- [ ] `GetSalesSessionSummaryQuery`
- [ ] `GetSalesSessionTotalsQuery`
- [ ] `ListSalesSessionsByOutletQuery`
- [ ] `ListSalesSessionsByUserQuery`
- [ ] `ListOpenSalesSessionsQuery`

## 4. Tenant/cashier HTTP endpoints

- [ ] `POST /tenant/sessions/open`
- [ ] `POST /tenant/sessions/{sessionId}/close`
- [ ] `GET /tenant/sessions/current`
  - current user from context, no terminal query param
- [ ] `GET /tenant/sessions/{sessionId}`
- [ ] `GET /tenant/sessions/{sessionId}/summary`
- [ ] `GET /tenant/sessions/{sessionId}/totals`
- [ ] Return `ApiResponse<T>`, not raw `ResponseEntity` for JSON.

## 5. Admin/Ops HTTP endpoints

- [ ] `GET /admin/sessions`
  - paginated
  - filters: status, outletId, userId, openedFrom/openedTo
- [ ] `GET /admin/sessions/open`
- [ ] `GET /admin/sessions/{sessionId}`
- [ ] `GET /admin/sessions/{sessionId}/totals`
- [ ] `POST /admin/sessions/{sessionId}/totals/recompute`
- [ ] Optional Ops:
  - `POST /admin/sessions/auto-open`
  - `POST /admin/sessions/auto-close`

## 6. Scheduler

- [ ] Add scheduler command only if outlet config enables it.
- [ ] Auto-open conditions:
  - outlet active
  - sales not blocked
  - configured autoOpenSession
  - seller/system user available
  - no current OPEN session
- [ ] Auto-close conditions:
  - configured autoCloseSession
  - end of operational window
  - no sale in progress
  - do not wait for payout
- [ ] Use BatchGate if scheduled.
- [ ] Use Clock, not Instant.now.

## 7. Controller cleanup

- [ ] Replace `TchContextResolver` with `@CurrentContext`.
- [ ] Request DTO uses typed IDs.
- [ ] Do not parse IDs manually in controller.
- [ ] Do not return domain model `SalesSessionTotals`.
- [ ] Map to response/view records.

## 8. Persistence

- [ ] Unique partial index for one OPEN session per seller.
- [ ] Index by tenant/outlet/user/status/openedAt.
- [ ] RLS policies.
- [ ] Mapper with typed IDs.

## 9. Tests

- [ ] Opening session when one already OPEN fails.
- [ ] Closing session preserves totals.
- [ ] Current session is by user, not terminal.
- [ ] Selling after close fails.
- [ ] Payout referencing closed selling session is allowed.
