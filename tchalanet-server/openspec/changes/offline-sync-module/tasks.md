# Tasks — Offline Sync Module

## Module setup

- [ ] Create/normalize `core.offlinesync.api` and `core.offlinesync.internal` structure.
- [ ] Add package-info Modulith module declaration.
- [ ] Ensure no offline sync code imports sales internals.

## Persistence

- [ ] Create/update offline submission table/entity.
- [ ] Add status, payload hash, device id, terminal id, outlet id, session id, seller id, receivedAt, saleOccurredAt.
- [ ] Add idempotency/deduplication constraints.
- [ ] Add indexes for tenant/status/receivedAt/device/terminal.
- [ ] Add RLS policies if tenant-scoped.

## Handlers/controllers

- [ ] Implement `SubmitOfflineSaleBatchController` or endpoint spec.
- [ ] Implement `SubmitOfflineSaleCommandHandler`.
- [ ] Implement `ValidateOfflineSubmissionCommandHandler`.
- [ ] Implement `PromoteOfflineSubmissionCommandHandler`.
- [ ] Implement `RejectOfflineSubmissionCommandHandler`.
- [ ] Implement `ListOfflineSubmissionsQueryHandler`.
- [ ] Implement `GetOfflineSubmissionDetailsQueryHandler`.
- [ ] Implement admin review controller if review flow exists.

## Integration

- [ ] Use operational context resolver for seller/terminal/outlet/session.
- [ ] Promote via `core.sales.api.command.SellTicketCommand` or dedicated offline promotion command.
- [ ] Publish events after commit.
- [ ] Optionally notify via `platform.notification`.

## Verification

- [ ] Duplicate submission is idempotent.
- [ ] Tampered hash/signature is rejected.
- [ ] Expired/off-cutoff sale is rejected through sales gate.
- [ ] Terminal/outlet/session mismatch is rejected.
- [ ] Valid submission is promoted once.
