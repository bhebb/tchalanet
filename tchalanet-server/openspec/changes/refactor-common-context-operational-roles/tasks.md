# Tasks — refactor-common-context-operational-roles

## 1. OpenSpec And Documentation

- [ ] Validate this change with `pnpm exec openspec validate refactor-common-context-operational-roles`.
- [ ] Update `docs/conventions/request_context_usage.md` with the canonical package organization and single-filter rule.
- [ ] Update `docs/conventions/user-contexte-operational.md` with attach-early/validate-late, role matrix and offline replay actor/seller distinction.
- [ ] Update `docs/modules/common.md` with `common.context` runtime-only boundaries.
- [ ] Update `docs/modules/platform.md` with tenant lookup ownership and super-admin override ownership.

## 2. Package Organization

- [ ] Ensure `TchContextFilter`, `CurrentContext`, `CurrentContextArgumentResolver`, `CurrentContextWebMvcConfig`, `ApiScopeResolver` and `TchRequestContextFactory` live under `common.context.web`.
- [ ] Ensure `TenantContextLookup` and `TenantContextResolver` live under `common.context.tenant`.
- [ ] Ensure system context types live under `common.context.system`.
- [ ] Create or normalize `common.context.operational` for neutral operational context types only.
- [ ] Move or bridge existing flat operational classes without breaking current callers.
- [ ] Keep imports backward-compatible during the first migration pass where feasible.

## 3. Operational Types And Bridge

- [ ] Add `OperationalContextRole` with `SELLER`, `ADMIN`, `SUPER_ADMIN`, `SYSTEM`, `NONE`.
- [ ] Keep `TrustLevel` values as `NONE`, `WEAK`, `STRONG`.
- [ ] Normalize `OperationalContextSource` around `NONE`, `CLIENT_CLAIM`, `SERVER_BOOTSTRAP`, `SIGNED_DEVICE_BINDING`, `ADMIN_SELECTION`, `SUPER_ADMIN_OVERRIDE`.
- [ ] Add typed POS operational context with `terminalId`, `outletId`, `salesSessionId`, `sellerUserId`, `role`, `source`, `trustLevel`.
- [ ] Add seller/admin/super-admin typed contexts or views as needed by the helpers.
- [ ] Keep the current flat `OperationalRequestContext` as a deprecated bridge once typed replacements exist.
- [ ] Add a migration note near the bridge explaining removal conditions.

## 4. Headers And Parser

- [ ] Add `OperationalContextHeaders` constants for `X-Tch-Terminal-Id`, `X-Tch-Outlet-Id`, `X-Tch-Sales-Session-Id`, `X-Tch-Operational-Source`, `X-Tch-Tenant-Override`, `X-Tch-Override-Reason`.
- [ ] Add a pure `OperationalContextHeaderParser` under `common.context.web` or an equivalent pure parser package.
- [ ] Parse blank operational headers as absent.
- [ ] Parse client-provided terminal/outlet/session headers as `CLIENT_CLAIM` and `WEAK` by default.
- [ ] Ensure parser code performs no repository lookup, permission check, terminal validation, outlet validation or session validation.
- [ ] Add tests for no headers, client claim, signed/server/admin selection source and malformed ids.

## 5. TchRequestContext Helpers

- [ ] Add typed helper for requiring POS operational context.
- [ ] Add typed helper for requiring trusted/strong POS operational context.
- [ ] Add typed helper for seller POS context.
- [ ] Add typed helper for admin POS context.
- [ ] Add typed helper for super-admin override context.
- [ ] Preserve current flat helper behavior during the bridge period.
- [ ] Add tests for missing context, weak context, wrong role and correct role.

## 6. Tenant Context Lookup

- [ ] Confirm `TenantContextLookup` remains in `common.context.tenant`.
- [ ] Confirm `TenantConfigContextLookup` remains in `platform.tenantconfig.internal.context`.
- [ ] Document that the lookup resolves a tenant from an already extracted id/code and does not put routing business logic in `common`.
- [ ] Add or update tests for tenant lookup integration if current coverage is missing.

## 7. Super-Admin Override

- [ ] Add request-scoped override handling for `X-Tch-Tenant-Override`.
- [ ] Require `X-Tch-Override-Reason` for sensitive tenant-scoped operations when override is active.
- [ ] Specify and enforce permission `platform.tenant.override`.
- [ ] Ensure override does not persist beyond the current request.
- [ ] Ensure audit receives actor id, target tenant id, reason, request id and correlation id whenever override is active.
- [ ] Add tests for missing reason, missing permission, valid override and audit emission.

## 8. Admin POS Selection Contract

- [ ] Define read-side contract for active admin POS selection.
- [ ] Provide a V1 stub returning no active selection if persistence/endpoints are not implemented in this change.
- [ ] Specify but do not implement `POST /tenant/me/operational-context/select`.
- [ ] Specify but do not implement `GET /tenant/me/operational-context`.
- [ ] Specify but do not implement `DELETE /tenant/me/operational-context`.
- [ ] Ensure admin does not become seller unless a use case explicitly accepts admin POS mode.

## 9. Core Validation Migration

- [ ] Keep terminal/outlet/session orchestration in the owning domain validators for V1.
- [ ] Migrate `core.sales` sensitive flows to typed POS context helpers.
- [ ] Migrate `core.payout` sensitive flows to typed POS context helpers.
- [ ] Migrate `core.offlinesync` sensitive flows while preserving `actorUserId` versus `sellerUserId`.
- [ ] Migrate `core.session` POS/session flows to typed context helpers.
- [ ] Do not introduce `core.operationalcontext`.
- [ ] Do not introduce `platform.operationalcontext`.
- [ ] Re-evaluate duplication only after real migrations expose repeated code with identical policy.

## 10. ArchUnit And Regression Tests

- [ ] Add ArchUnit rule that `common.context` must not import platform/core/catalog/features.
- [ ] Add ArchUnit rule that `common.context.operational` must not import repositories.
- [ ] Add ArchUnit rule that `common.context.operational` must not import `CommandBus` or `QueryBus`.
- [ ] Add ArchUnit rule preventing a class named `OperationalContextFilter`.
- [ ] Add ArchUnit rule preventing a package named `platform.usercontext`.
- [ ] Add ArchUnit guard against new usages of the deprecated flat bridge outside the migration allowance.
- [ ] Run the narrowest relevant Maven module tests after implementation.
- [ ] Run `./mvnw clean verify` before completing the implementation change.
