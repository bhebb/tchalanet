# Tasks — refactor-common-context-operational-roles

## 1. OpenSpec And Documentation

- [x] Validate this change with `pnpm exec openspec validate refactor-common-context-operational-roles`.
- [x] Update `docs/conventions/request_context_usage.md` with the canonical package organization and single-filter rule.
- [x] Update `docs/conventions/user-contexte-operational.md` with attach-early/validate-late, role matrix and offline replay actor/seller distinction.
- [x] Update `docs/modules/common.md` with `common.context` runtime-only boundaries.
- [x] Update `docs/modules/platform.md` with tenant lookup ownership and super-admin override ownership.

## 2. Package Organization

- [x] Ensure `TchContextFilter`, `CurrentContext`, `CurrentContextArgumentResolver`, `CurrentContextWebMvcConfig`, `ApiScopeResolver` and `TchRequestContextFactory` live under `common.context.web`.
- [x] Ensure `TenantContextLookup` and `TenantContextResolver` live under `common.context.tenant`.
- [x] Ensure system context types live under `common.context.system`.
- [x] Create or normalize `common.context.operational` for neutral operational context types only.
- [x] Move or bridge existing flat operational classes without breaking current callers.
- [x] Keep imports backward-compatible during the first migration pass where feasible.

## 3. Operational Types And Bridge

- [x] Add `OperationalContextRole` with `SELLER`, `ADMIN`, `SUPER_ADMIN`, `SYSTEM`, `NONE`.
- [x] Keep `TrustLevel` values as `NONE`, `WEAK`, `STRONG`.
- [x] Normalize `OperationalContextSource` around `NONE`, `CLIENT_CLAIM`, `SERVER_BOOTSTRAP`, `SIGNED_DEVICE_BINDING`, `ADMIN_SELECTION`, `SUPER_ADMIN_OVERRIDE`.
- [x] Add typed POS operational context with `terminalId`, `outletId`, `salesSessionId`, `sellerUserId`, `role`, `source`, `trustLevel`.
- [x] Add seller/admin/super-admin typed contexts or views as needed by the helpers.
- [x] Keep the current flat `OperationalRequestContext` as a deprecated bridge once typed replacements exist.
- [x] Add a migration note near the bridge explaining removal conditions.

## 4. Headers And Parser

- [x] Add `OperationalContextHeaders` constants for `X-Tch-Terminal-Id`, `X-Tch-Outlet-Id`, `X-Tch-Sales-Session-Id`, `X-Tch-Operational-Source`, `X-Tch-Tenant-Override`, `X-Tch-Override-Reason`.
- [x] Add a pure `OperationalContextHeaderParser` under `common.context.web` or an equivalent pure parser package.
- [x] Parse blank operational headers as absent.
- [x] Parse client-provided terminal/outlet/session headers as `CLIENT_CLAIM` and `WEAK` by default.
- [x] Ensure parser code performs no repository lookup, permission check, terminal validation, outlet validation or session validation.
- [x] Add tests for no headers, client claim, signed/server/admin selection source and malformed ids.

## 5. TchRequestContext Helpers

- [x] Add typed helper for requiring POS operational context.
- [x] Add typed helper for requiring trusted/strong POS operational context.
- [x] Add typed helper for seller POS context.
- [x] Add typed helper for admin POS context.
- [x] Add typed helper for super-admin override context.
- [x] Preserve current flat helper behavior during the bridge period.
- [x] Add tests for missing context, weak context, wrong role and correct role.

## 6. Tenant Context Lookup

- [x] Confirm `TenantContextLookup` remains in `common.context.tenant`.
- [x] Confirm `TenantConfigContextLookup` remains in `platform.tenantconfig.internal.context`.
- [x] Document that the lookup resolves a tenant from an already extracted id/code and does not put routing business logic in `common`.
- [x] Add or update tests for tenant lookup integration if current coverage is missing.

## 7. Super-Admin Override

- [x] Add request-scoped override handling for `X-Tch-Tenant-Override`.
- [x] Require `X-Tch-Override-Reason` for sensitive tenant-scoped operations when override is active.
- [x] Specify and enforce permission `platform.tenant.override`.
- [x] Ensure override does not persist beyond the current request.
- [x] Ensure audit receives actor id, target tenant id, reason, request id and correlation id whenever override is active.
- [x] Add tests for missing reason, missing permission, valid override and audit emission.

## 8. Admin POS Selection Contract

- [x] Define read-side contract for active admin POS selection.
- [x] Provide a V1 stub returning no active selection if persistence/endpoints are not implemented in this change.
- [x] Specify but do not implement `POST /tenant/me/operational-context/select`.
- [x] Specify but do not implement `GET /tenant/me/operational-context`.
- [x] Specify but do not implement `DELETE /tenant/me/operational-context`.
- [x] Ensure admin does not become seller unless a use case explicitly accepts admin POS mode.

## 9. Core Validation Migration

- [x] Keep terminal/outlet/session orchestration in the owning domain validators for V1.
- [x] Migrate `core.sales` sensitive flows to typed POS context helpers.
- [x] Migrate `core.payout` sensitive flows to typed POS context helpers.
- [x] Migrate `core.offlinesync` sensitive flows while preserving `actorUserId` versus `sellerUserId`.
- [x] Migrate `core.session` POS/session flows to typed context helpers.
- [x] Do not introduce `core.operationalcontext`.
- [x] Do not introduce `platform.operationalcontext`.
- [x] Re-evaluate duplication only after real migrations expose repeated code with identical policy.

## 10. ArchUnit And Regression Tests

- [x] Add ArchUnit rule that `common.context` must not import platform/core/catalog/features.
- [x] Add ArchUnit rule that `common.context.operational` must not import repositories.
- [x] Add ArchUnit rule that `common.context.operational` must not import `CommandBus` or `QueryBus`.
- [x] Add ArchUnit rule preventing a class named `OperationalContextFilter`.
- [x] Add ArchUnit rule preventing a package named `platform.usercontext`.
- [x] Add ArchUnit guard against new usages of the deprecated flat bridge outside the migration allowance.
- [x] Run the narrowest relevant Maven module tests after implementation.
- [ ] Run `./mvnw clean verify` before completing the implementation change.
