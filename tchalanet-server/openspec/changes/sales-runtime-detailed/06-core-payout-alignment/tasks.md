# Tasks

## 1. Domain model

- [ ] Refactor `Payout`.
- [ ] Fields:
  - PayoutId
  - TenantId
  - TicketId
  - sellingOutletId
  - sellingSessionId
  - payingOutletId nullable
  - payingSessionId nullable
  - terminalId nullable
  - paidByUserId
  - amountCents
  - currency
  - status REQUESTED/APPROVED/PAID/REJECTED/CANCELLED
- [ ] Invariant:
  - cannot pay twice
  - can pay after selling session closed
  - payout amount equals winning amount unless override is explicit/audited

## 2. Commands

- [ ] `RegisterPayoutCommand`
- [ ] `ApprovePayoutCommand`
- [ ] `RejectPayoutCommand`
- [ ] `ExecutePayoutCommand`
- [ ] `CancelPayoutCommand`
- [ ] `RecomputePayoutEligibilityCommand` optional

## 3. Queries / read endpoints

Do not rely only on overview.

- [ ] `GetPayoutByIdQuery`
- [ ] `ListPayoutsQuery`
- [ ] `ListPayoutsByTicketQuery`
- [ ] `ListPendingPayoutsQuery`
- [ ] `GetPayoutEligibilityForTicketQuery`
- [ ] `GeneratePayoutReportQuery`

## 4. Tenant/Admin HTTP endpoints

- [ ] `POST /tenant/payouts/request` if seller can request
- [ ] `POST /tenant/payouts/{payoutId}/execute` if seller can pay
- [ ] `GET /tenant/payouts/{payoutId}`
- [ ] `GET /tenant/payouts`
- [ ] `GET /admin/payouts`
- [ ] `GET /admin/payouts/{payoutId}`
- [ ] `POST /admin/payouts/{payoutId}/approve`
- [ ] `POST /admin/payouts/{payoutId}/reject`
- [ ] `POST /admin/payouts/{payoutId}/execute`
- [ ] `GET /admin/payouts/report`

## 5. Controller cleanup

- [ ] Remove `tenantId` from request bodies.
- [ ] Use `@CurrentContext`.
- [ ] Use actor from context.
- [ ] Return `ApiResponse<T>` for JSON.
- [ ] File report endpoints may return Resource.
- [ ] Fix security annotations; no placeholder `hasAuthority('local')`.

## 6. Policy integration

- [ ] Evaluate payout limit/autonomy.
- [ ] Create approval request if needed.
- [ ] Allow payout in outlet different from selling outlet if permission/policy allows.
- [ ] Audit all approve/reject/execute.

## 7. Events

- [ ] Publish `PayoutRequestedEvent`, `PayoutApprovedEvent`, `PayoutPaidEvent`, `PayoutRejectedEvent` after commit.
- [ ] Update stats/projections via idempotent listeners.

## 8. Tests

- [ ] Payout allowed after selling session closed.
- [ ] Double payout blocked.
- [ ] Payout requires WON_UNCLAIMED ticket.
- [ ] Approval required when policy says so.
- [ ] Report query filters by date/outlet/status.
