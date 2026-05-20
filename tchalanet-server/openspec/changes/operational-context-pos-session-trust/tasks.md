# Tasks: operational-context-pos-session-trust

## A. common.context primitives

- [x] A.1 Add `OperationalContextSource { NONE, CLIENT_CLAIM, SIGNED_DEVICE_BINDING, ADMIN_SELECTION }`.
- [x] A.2 Add `OperationalContextTrust { NONE, WEAK, STRONG }`.
- [x] A.3 Remove any `OperationalContextSource(TrustLevel)` constructor/coupling.
- [x] A.4 Add `OperationalContextHeaders` with the six canonical header names.
- [x] A.5 Update `OperationalContextHint` to carry terminal/outlet/session/source/trust.
- [x] A.6 Add `TchRequestContext.operationalContext()` and `operationalContextRequired()`.
- [x] A.7 Remove/avoid `TchRequestContext.trustedOperationalContextRequired()`.

## B. TchContextFilter / resolver

- [x] B.1 Ensure `TchContextFilter` is the single HTTP context producer.
- [x] B.2 Parse POS context IDs only from headers, never from body.
- [x] B.3 Parse tenant override headers separately from POS operational source.
- [x] B.4 Derive `OperationalContextTrust` server-side only.
- [x] B.5 Ignore or reject `X-Tch-Operational-Trust`; it must not affect trust.
- [x] B.6 `SIGNED_DEVICE_BINDING` without valid proof returns 401.
- [x] B.7 `ADMIN_SELECTION` without signed server selection token downgrades to `CLIENT_CLAIM/WEAK` in V1 and logs `tch.context.admin-selection-without-token`.
- [ ] B.8 Implement body guard for POS IDs in protected POS request bodies: return 400 `operational_context.in_body`.

## C. Tenant override

- [x] C.1 Implement/confirm `X-Tch-Tenant-Override` is only accepted for SUPER_ADMIN.
- [x] C.2 Require permission `platform.tenant.override`.
- [x] C.3 Require non-blank `X-Tch-Override-Reason`.
- [x] C.4 Capture override metadata for audit.
- [x] C.5 Ensure `SUPER_ADMIN_OVERRIDE` is not added to `OperationalContextSource`.

## D. core.session API

- [x] D.1 Add `core.session.api.query.ResolvePosOperationContextQuery`.
- [x] D.2 Add `core.session.api.query.PosOperationAction`.
- [x] D.3 Add `core.session.api.model.ValidatedPosOperationContext`.
- [x] D.4 Implement `ResolvePosOperationContextQueryHandler` in `core.session.internal.application.query.handler`.
- [x] D.5 Validate terminal/outlet/session/user/source/action in the documented fail-fast order.
- [x] D.6 Use `QueryBus.ask(...)` from callers.
- [x] D.7 Delete/migrate any `core.terminal.GetCurrentOperationalContextQuery` equivalent.

## E. PosActionPolicy

- [x] E.1 Add `PosActionPolicy` as the single mapping from action to minimum trust.
- [x] E.2 Include all actions: `ADMIN_POS_SELL`, `SELL_TICKET_ONLINE`, `REQUEST_OFFLINE_GRANT`, `SYNC_OFFLINE_SALES`, `PAYOUT`, `CLOSE_SESSION`.
- [x] E.3 No silent default; unmapped action fails startup/test.
- [ ] E.4 Add feature flag `pos.seller.require-strong-trust` for seller sale migration.

## F. core.offlinesync grant

- [x] F.1 Update `IssueOfflineSalesGrantCommandHandler` to call `ResolvePosOperationContextQuery` with `REQUEST_OFFLINE_GRANT`.
- [x] F.2 Reject `CLIENT_CLAIM/WEAK` if no server-verifiable device proof exists.
- [ ] F.3 Validate tenant offline enabled, terminal/offline allowed, outlet allowed, seller allowed, device binding, no incompatible active grant, quota available, duration allowed.
- [ ] F.4 Persist `OfflineGrant` with terminal/outlet/session/user/device/limits/token hash.
- [x] F.5 Emit `OfflineGrantIssuedEvent` after commit.

## G. core.offlinesync submissions

- [x] G.1 Add/confirm `offline_submission` as distinct from ticket.
- [ ] G.2 Validate grant + device proof + signature for sync.
- [x] G.3 Store submissions before creating tickets.
- [ ] G.4 Accepted submissions call `core.sales` through command/API to create the real ticket.
- [ ] G.5 Rejected/review submissions never create tickets.

## H. Audit

- [ ] H.1 Sensitive actions audit `ValidatedPosOperationContext`, not raw `OperationalContextHint`.
- [ ] H.2 Audit includes tenant override metadata when used.
- [ ] H.3 Audit includes operational source/trust/action/result.
- [ ] H.4 Add audit tests for admin POS WEAK accepted and offline grant WEAK rejected.

## I. ArchUnit / guardrails

- [ ] I.1 Forbid `OperationalContextTrust` assignment outside resolver/test fixtures.
- [ ] I.2 Forbid direct trust checks outside `PosActionPolicy`.
- [ ] I.3 Forbid `SUPER_ADMIN_OVERRIDE` in `OperationalContextSource`.
- [ ] I.4 Forbid POS-frame IDs in POS request DTO bodies.
- [ ] I.5 Forbid sensitive handlers using `OperationalContextHint` after `ValidatedPosOperationContext` should be resolved.

## J. Tests

- [x] J.1 Resolver: `CLIENT_CLAIM` -> WEAK.
- [ ] J.2 Resolver: valid signed device binding -> STRONG.
- [x] J.3 Resolver: invalid signed device binding -> 401.
- [x] J.4 Resolver: `ADMIN_SELECTION` without token -> downgrade to WEAK + warn.
- [x] J.5 `PosActionPolicy` covers all enum values.
- [ ] J.6 Admin POS with WEAK succeeds only after core.session validation.
- [x] J.7 Offline grant with WEAK only fails.
- [ ] J.8 Offline grant with STRONG and valid device proof can issue grant.
- [ ] J.9 Body containing terminal/outlet/session IDs returns 400.
- [ ] J.10 `queryBus.ask(...)` is used for POS resolution.

## K. Verification

- [x] K.1 `openspec validate operational-context-pos-session-trust` passes.
- [x] K.2 Targeted backend tests pass.
- [ ] K.3 Full backend verification passes before merge.
- [ ] K.4 Search confirms no body POS IDs in tenant POS request DTOs.
- [x] K.5 Search confirms no stale `trustedOperationalContextRequired()` usage.
