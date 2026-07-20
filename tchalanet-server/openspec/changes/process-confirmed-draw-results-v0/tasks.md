# Tasks

## 1. Contract and safety boundary

- [x] Document the result-state boundary and replay model.
- [x] Add focused tests proving provisional results cannot attach/apply to tenant draws.
- [x] Gate both the scheduler candidate query and apply handler on `CONFIRMED | OVERRIDDEN`.
- [x] Ensure confirmation makes a formerly provisional result eligible for the normal apply pass.

## 2. Replayable ticket processing

- [x] Add a bounded pending-ticket reader and result-command outcome that reports remaining work.
- [x] Preserve per-ticket failure diagnostics without treating skipped failures as completed work.
- [x] Make the applied-result listener mark delivery complete only when the bounded command has no unresolved failure/work.
- [x] Add a scheduler recovery path for resulted draws with pending ticket work.
- [x] Add tests for chunking, replay, no duplicate payout events, and per-ticket failure recovery.

## 3. Draw settlement and provisional reminders

- [x] Settle a draw only when ticket processing reports no remaining eligible tickets.
- [x] Remove the separate `settle_draws` scheduler/batch invocation while keeping explicit ops settlement semantics.
- [x] Convert the provisional watchdog to the existing action-required notification/Slack path with idempotent correlation.
- [x] Add tests for provisional reminder, eventual resolution, and no financial side effects before confirmation.

## 4. Verification and documentation

- [x] Update draw, draw-result, sales, analytics and batch documentation.
- [x] Run focused unit/integration tests, compile, formatting and strict OpenSpec validation.
- [ ] Add canonical E2E coverage for provider-result confirmation: a provisional provider result
      creates no tenant result-available notification, no ticket result, no payout and no analytics;
      Ops confirmation then emits the targeted tenant notification and enables normal apply/settle.
