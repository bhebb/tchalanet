# Tasks: simplify-draw-scheduler-v0

## 1. Opening policy

- [x] Confirm `DrawSchedulerWindows` has no consumers and is disabled in runtime configuration.
- [x] Define the V0 daily generation and global opening times in the operating timezone.
- [x] Replace the provider-local opening path with the bounded upcoming-draw command path.
- [x] Preserve provider-calendar cancellation and idempotent bulk-open behavior.

## 2. Runtime configuration

- [x] Remove the unused windows configuration/component.
- [x] Add a scheduler operating timezone and explicit daily opening horizon/lag configuration.
- [x] Set generation to 00:05 and opening to 00:15 in `America/Port-au-Prince` by default.
- [x] Keep processing at five-minute bounded candidate batches.

## 3. Batch contract and verification

- [x] Align `open_draws` batch parameters and allowlist with the upcoming-draw command.
- [x] Remove obsolete command, handler, port, repository queries, and test stubs.
- [x] Add or update focused tests for the global opening policy.
- [x] Run focused core/app compilation and tests.
- [x] Run OpenSpec strict validation.
