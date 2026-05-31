# tasks.md — terminal-reorg-security

Read this file at the start of each session. Resume at the first unchecked task. Check each task immediately after completing it — do not batch.

## Progress

- [x] 00 — controller cleanup (`core.terminal` admin + tenant controllers split by responsibility)
- [x] 01 — terminal binding schema + activation challenge (entity, challenge table, verify handler, public key material on binding)
- [x] 02 — terminal device proof verification (`TerminalDeviceProofVerifier`, `TerminalSignaturePayloadCanonicalizerV1`, `TerminalNonceReplayGuard`, `terminal_device_nonce` table)
- [x] 03 — `platform.keymanagement` minimal V1 (config-backed `Ed25519ServerSigningService`, `BackendPublicKeysController`, `KeyManagementProperties`)
- [x] 04 — backend-signed offline grant (`core.offlinesync` creates grant, calls `platform.keymanagement.ServerSigningApi`, returns signed response)
- [x] 05 — integrate device proof into sell / payout confirm / offline grant request / offline sync
- [ ] 06 — tests: unit + integration + RLS + E2E (see `tasks/04-tests-matrix.md`)
- [x] 07 — docs update + `follow-up-mobile.md` mobile contract + open `tchalanet-mobile/openspec/changes/terminal-device-proof-client/`

## Task file index

| Task | Detail |
|---|---|
| 00 | `tasks/00-cleanup-terminal-controllers.md` |
| 01 | `tasks/01-terminal-challenge-binding.md` |
| 02 | `tasks/02-device-proof-signatures.md` |
| 03 | `tasks/03-signed-offline-grants.md` (see also `specs/platform-keymanagement/spec.md`) |
| 04 | `tasks/03-signed-offline-grants.md` §3 |
| 05 | `tasks/02-device-proof-signatures.md` §hook-into-handlers |
| 06 | `tasks/04-tests-matrix.md` |
| 07 | `follow-up-mobile.md` |

## Ordering constraints

- 00 has no dependencies.
- 01 requires 00 (binding uses cleaned-up challenge/controller structure).
- 02 requires 01 (device proof needs binding + public key).
- 03 is independent of 01–02 but must complete before 04.
- 04 requires 02 + 03.
- 05 requires 02 (device proof query must exist before hooking).
- 06 requires 01–05.
- 07 can begin in parallel with 06.
