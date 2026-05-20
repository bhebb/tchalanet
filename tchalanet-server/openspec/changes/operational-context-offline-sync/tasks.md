# Tasks — operational-context-offline-sync

## 1. Context pipeline consolidation (common.context, common.security)

- [x] Add `TrustLevel` enum (`NONE`, `WEAK`, `STRONG`) in `common.context`.
- [x] Refactor `OperationalContextSource` to carry a `TrustLevel`; rewrite `isTrustedForSensitiveOperation()` as `trustLevel == STRONG`.
- [x] Remove `selectedByAdmin` field from `OperationalRequestContext`; update every reader to use `source == ADMIN_SELECTION`.
- [x] Add `TchRequestContext.trustedOperationalContextRequired(): OperationalRequestContext` helper.
- [x] Create `OperationalContextResolver` interface + default impl in `common.security`, injecting `QueryBus` to call `GetCurrentOperationalContextQuery` with role rules.
- [x] Modify `TchContextFilter` to invoke the resolver between `actorContextResolver` and `contextBinder.bind`.
- [x] **Delete** `OperationalContextFilter.java` + remove its `@Component`/`SecurityConfig`/`FilterRegistrationBean` wiring.
- [x] Implement `GetCurrentOperationalContextQueryHandler` in `core.terminal.application.query.handler`.

## 3. Aggregate fields + Flyway (core.session, core.outlet)

- [x] Add `finalizedAt`, `finalizedBy` to `SalesSession` domain record + JPA entity + mapper.
- [x] Edit the existing pre-go-live `V*__sales_session*.sql` Flyway script to add those columns.
- [x] Add `payoutBlocked`, `payoutBlockedReason`, `offlineSalesBlocked`, `offlineSalesBlockedReason` to `Outlet` domain record + mapper (JPA entity already has them).
- [x] Edit the existing pre-go-live `V*__outlet*.sql` Flyway script if columns are missing.

## 4. Missing validators (core.sales, core.payout)

- [x] Create `PosCancelOperationValidator` in `core.sales.application.validation` (Terminal CANCEL + Session CANCEL).
- [x] Create `PosPayoutOperationValidator` in `core.payout.application.validation` (Terminal PAYOUT + Outlet PAYOUT + Session SELL/PAYOUT).
- [x] Create `OfflineSaleAcceptanceValidator` in `core.sales.application.validation`:
  - FINALIZED session → `SALES_REVIEW_REQUIRED` + `SESSION_FINALIZED` + flag `FINALIZED_SESSION`
  - draw result known → `RESULT_KNOWN_AT_SYNC` flag
  - device-time policy
- [ ] Wire every critical handler to call `trustedOperationalContextRequired()` then its validator.

## 5. Concurrency hardening (core.sales, core.payout, core.offlinesync)

- [x] Add in-tx re-check of `terminal.locked`, outlet block flags, `session.status` in:
  - `SellPosTicketCommandHandler` (Javadoc concurrency contract)
  - `RegisterPayoutCommandHandler` (Javadoc concurrency contract)
  - `ReceiveOfflineBatchCommandHandler` (Javadoc concurrency contract)
  - `ApproveOfflineSubmissionCommandHandler` (Javadoc concurrency contract)
- [ ] `IssueOfflineGrantCommandHandler` — pre-existing stub, pending draw wiring
- [ ] `SyncOfflineSalesCommandHandler` — pending ticket model wiring
- [x] Document the race strategy as Javadoc on each handler.

## 6. Offline sync domain events (core.offlinesync)

- [x] Add `OfflineGrantIssuedEvent`, `OfflineBatchReceivedEvent`, `OfflineSubmissionTechnicallyRejectedEvent`, `OfflineBatchReadyForSalesEvent`, `OfflineSubmissionSalesDecisionRecordedEvent`.
- [x] Publish after commit from their respective handlers (`ReceiveOfflineBatchCommandHandler`, `ApproveOfflineSubmissionCommandHandler`).
- [x] Add `riskFlags` on `OfflineSaleSubmission` with values `FINALIZED_SESSION`, `RESULT_KNOWN_AT_SYNC`.

## 7. Complete approve-offline-submission flow

- [x] `ApproveOfflineSubmissionCommandHandler`: record ACCEPTED decision + publish `OfflineSubmissionSalesDecisionRecordedEvent` after commit.
- [ ] Create core.sales event listener for `OfflineSubmissionSalesDecisionRecordedEvent` → create Ticket + publish `TicketPlacedEvent` (blocked on ticket model wiring in SyncOfflineSalesCommandHandler).

## 8. Tests

- [ ] Unit: `OperationalContextSource.trustLevel` + `trustedOperationalContextRequired`.
- [ ] Integration: existing controllers still get operational context after `OperationalContextFilter` removal (Testcontainers + RLS).
- [ ] Validator unit tests covering each fail-fast position.
- [ ] Offline-sync acceptance test: submission against `FINALIZED` session → status `SALES_REVIEW_REQUIRED`, reason `SESSION_FINALIZED`, `riskFlags` ⊇ `{FINALIZED_SESSION}`, no `TicketPlacedEvent`.

## 9. Verification

- [ ] `cd tchalanet-server && openspec validate operational-context-offline-sync` passes.
- [ ] `./gradlew :tchalanet-server:test` green.
- [ ] `./gradlew :tchalanet-server:integrationTest` green.
- [ ] Manual: terminal locked between validate and commit → in-tx re-check rejects with `TERMINAL_LOCKED`.
