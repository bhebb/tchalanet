# Tasks

## Discovery

- [x] Read backend/web routers and OpenSpec workflow.
- [x] Inspect `platform.audit` near-code documentation.
- [x] Inspect the superadmin audit page and Angular API client.
- [x] Inspect backend audit controller, service, repository adapter and persistence model.
- [x] Inspect RLS policies and API scope resolution for `/platform/audit/logs`.
- [x] Identify the two audit families: functional audit and Envers technical revision audit.

## Debug

- [x] Run the local stack and query `audit_event` count.
- [x] Insert manual smoke data into `audit_event` and `revinfo`/`result_slot_aud`.
- [x] Verify superadmin platform RLS can see the manual `audit_event` row.
- [x] Call `GET /api/v1/platform/audit/logs` as superadmin and compare API payload with DB count.
- [x] Execute one known `@AuditLog` action and verify a new `audit_event` row.
- [x] Execute one seller-terminal create action and verify both `audit_event` and `seller_terminal_aud` rows.
- [x] Fix the local Envers schema mismatch for seller-terminal PIN-reset audited fields.
- [x] Add missing `SellerTerminalId` path-variable converter and retest seller-terminal block update.
- [x] Create `platform.entityhistory` slice boundary for Envers revision entity/listener ownership.
- [x] Restrict Envers entity coverage and fresh-database audit migrations to `SELLER_TERMINAL`, `DRAW_RESULT`, and `LIMIT_ASSIGNMENT`.
- [ ] Verify live RLS variables for the real superadmin audit HTTP request.
- [ ] Add/adjust focused backend tests once the failing runtime condition is confirmed.

## Proposal

- [x] Document current findings.
- [x] Propose the short-term functional audit fix path.
- [x] Propose the future Envers read-only usage model.
- [x] Separate future functional audit and entity revision permissions/contracts.

## Implementation Candidates

- [ ] If data is empty: update web copy/empty state to clarify functional audit vs Envers revisions.
- [x] Add a separate platform operations web entry and page for allowlisted entity revision history.
- [ ] If API filters data: fix superadmin platform RLS/context handling and add integration tests.
- [ ] If writes are missing: capture actor/tenant at audit aspect time or extend `LogAuditEventRequest` with an actor/context snapshot.
- [ ] Add a future OpenSpec for `platform.entityhistory` read-only revision projections before exposing Envers in the UI.
