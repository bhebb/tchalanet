# Tasks — operational-context-offline-sync

## 1. Context pipeline consolidation (common.context, common.security)

- [ ] Add `TrustLevel` enum (`NONE`, `WEAK`, `STRONG`) in `common.context`.
- [ ] Refactor `OperationalContextSource` to carry a `TrustLevel`; rewrite `isTrustedForSensitiveOperation()` as `trustLevel == STRONG`.
- [ ] Remove `selectedByAdmin` field from `OperationalRequestContext`; update every reader to use `source == ADMIN_SELECTION`.
- [ ] Add `TchRequestContext.trustedOperationalContextRequired(): OperationalRequestContext` helper.
- [ ] Create `OperationalContextResolver` interface + default impl in `common.security`, injecting `QueryBus` to call `GetCurrentOperationalContextQuery` with role rules.
- [ ] Modify `TchContextFilter` to invoke the resolver between `actorContextResolver` and `contextBinder.bind`.
- [ ] **Delete** `OperationalContextFilter.java` + remove its `@Component`/`SecurityConfig`/`FilterRegistrationBean` wiring.
- [ ] Implement `GetCurrentOperationalContextQueryHandler` in `core.terminal.application.query.handler`.

## 2. Admin POS selection (common.security + core.terminal)

- [ ] Add `OperationalContextSelectionController`: `POST/GET/DELETE /tenant/me/operational-context[/select]`.
- [ ] Add `SelectAdminOperationalContextCommand` + handler; persist short-TTL row keyed by `(tenantId, userId)` with `expiresAt`.
- [ ] Audit every selection through the existing audit port.
- [ ] Resolver: for `TENANT_ADMIN`/`SUPER_ADMIN`, read admin selection first, return `ADMIN_SELECTION`.

## 3. Aggregate fields + Flyway (core.session, core.outlet)

- [ ] Add `finalizedAt`, `finalizedBy` to `SalesSession` domain record + JPA entity + mapper.
- [ ] Edit the existing pre-go-live `V*__sales_session*.sql` Flyway script to add those columns.
- [ ] Add `payoutBlocked`, `payoutBlockedReason`, `offlineSalesBlocked`, `offlineSalesBlockedReason` to `Outlet` domain record + mapper (JPA entity already has them).
- [ ] Edit the existing pre-go-live `V*__outlet*.sql` Flyway script if columns are missing.

## 4. Missing validators (core.sales, core.payout)

- [ ] Create `PosCancelOperationValidator` in `core.sales.application.validation` (Terminal CANCEL + Session CANCEL).
- [ ] Create `PosPayoutOperationValidator` in `core.payout.application.validation` (Terminal PAYOUT + Outlet PAYOUT + Session SELL/PAYOUT).
- [ ] Create `OfflineSaleAcceptanceValidator` in `core.sales.application.validation`:
  - FINALIZED session → `SALES_REVIEW_REQUIRED` + `SESSION_FINALIZED` + flag `FINALIZED_SESSION`
  - draw result known → `RESULT_KNOWN_AT_SYNC` flag
  - device-time policy
- [ ] Wire every critical handler to call `trustedOperationalContextRequired()` then its validator.

## 5. Concurrency hardening (core.sales, core.payout, core.offlinesync)

- [ ] Add in-tx re-check of `terminal.locked`, outlet block flags, `session.status` in:
  - `SellPosTicketCommandHandler`
  - `RequestPayoutCommandHandler`
  - `IssueOfflineGrantCommandHandler`
  - `ReceiveOfflineBatchCommandHandler`
  - `SyncOfflineSalesCommandHandler`
  - `AdminApproveOfflineSubmissionCommandHandler`
- [ ] Document the race strategy as Javadoc on each handler.

## 6. Offline sync domain events (core.offlinesync)

- [ ] Add `OfflineGrantIssuedEvent`, `OfflineBatchReceivedEvent`, `OfflineSubmissionTechnicallyRejectedEvent`, `OfflineBatchReadyForSalesEvent`, `OfflineSubmissionSalesDecisionRecordedEvent`.
- [ ] Publish after commit from their respective handlers (existing `DomainEventPublisher` / outbox pattern).
- [ ] Add `riskFlags` on `OfflineSaleSubmission` with values `FINALIZED_SESSION`, `RESULT_KNOWN_AT_SYNC`.

## 7. Complete approve-offline-submission flow

- [ ] In `ApproveOfflineSubmissionCommandHandler`, on admin approval: emit `TicketPlacedEvent` through `core.sales` (no direct sales repository writes from `offlinesync`).

## 8. Tests

- [ ] Unit: `OperationalContextSource.trustLevel` + `trustedOperationalContextRequired`.
- [ ] Integration: existing controllers still get operational context after `OperationalContextFilter` removal (Testcontainers + RLS).
- [ ] Validator unit tests covering each fail-fast position.
- [ ] Offline-sync acceptance test: submission against `FINALIZED` session → status `SALES_REVIEW_REQUIRED`, reason `SESSION_FINALIZED`, `riskFlags` ⊇ `{FINALIZED_SESSION}`, no `TicketPlacedEvent`.
- [ ] Admin POS endpoints: permission + audit assertions.

## 9. Verification

- [ ] `cd tchalanet-server && openspec validate operational-context-offline-sync` passes.
- [ ] `./gradlew :tchalanet-server:test` green.
- [ ] `./gradlew :tchalanet-server:integrationTest` green.
- [ ] Manual: `POST /tenant/me/operational-context/select` as `TENANT_ADMIN` → subsequent sale OK; without selection → 403.
- [ ] Manual: terminal locked between validate and commit → in-tx re-check rejects with `TERMINAL_LOCKED`.
