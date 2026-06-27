# Tasks: SellerTerminal V0

## 1. Schema (migration V233) ✓

- [x] Inventory current terminal/user/seller/cashier tables.
- [x] Decide if `seller_terminal_external_identity` exists — No; créer table séparée (pattern app_user_external_identity).
- [x] Create `seller_terminal` table.
- [x] Add unique `(tenant_id, terminal_code)`.
- [x] Add unique external identity constraint `(external_provider, external_issuer, external_subject)`.
- [x] Add indexes: `(tenant_id, status)`, `(tenant_id, terminal_code)`, `(tenant_id, display_name)`, `(tenant_id, outlet_id)`.
- [x] Add ticket columns: `seller_terminal_id`, `seller_commission_rate_snapshot`, `seller_commission_amount_snapshot`.
- [x] Keep `outlet_id` nullable.
- [x] Keep sales session out of V0 path.
- [x] `revinfo` exists (V101).
- [x] Create `seller_terminal_aud` (partial — control/financial fields only) in V233.

## 2. Domain model

- [x] Add `SellerTerminalId` typed ID if missing — existed in tchalanet-common.
- [x] Add `SellerTerminalStatus` (PENDING / ACTIVE / BLOCKED / DISABLED) — in `core.terminal.api.model`.
- [x] Add `CommissionRate` value object or validate decimal range — validated inline in `SellerTerminal.createPending` / `updateCommissionRate` [0.00, 100.00].
- [x] Add `SellerTerminalJpaEntity` with partial Envers (field-level `@Audited(withModifiedFlag=true)` only; no class-level `@Audited`; no `@NotAudited` needed).
- [x] Add `SellerTerminalExternalIdentityJpaEntity` (Class B — no Envers).
- [x] Implement status transitions in `SellerTerminal` domain aggregate:
  - `createPending` — factory, PENDING status;
  - `activate` — PENDING/BLOCKED → ACTIVE;
  - `block` — any → BLOCKED;
  - `unblock` — BLOCKED → ACTIVE;
  - `disable` — any → DISABLED;
  - `resetAccessMetadata` — clears lastSeenAt.
- [x] Enforce:
  - disabled cannot become active — `activate()` throws `SellerTerminalStatusException` if DISABLED;
  - blocked cannot sell — `canSell()` returns false unless status == ACTIVE;
  - commission rate within accepted range — validated in `createPending` and `updateCommissionRate`.

## 3. Commands

- [x] `CreateSellerTerminalCommand`.
- [x] `UpdateSellerTerminalCommand`.
- [x] `BlockSellerTerminalCommand`.
- [x] `UnblockSellerTerminalCommand`.
- [x] `DisableSellerTerminalCommand`.
- [x] `ResetSellerTerminalAccessCommand`.
- [x] Add handlers with `@UseCase`.
- [x] Add `@TchTx` to write handlers.
- [x] Add business `audit_event` declarations (all 7 events) — added `SELLER_TERMINAL` to `AuditEntityType`; added 7 `SELLER_TERMINAL_*` entries to `AuditAction`; `@AuditLog` goes on controller methods (S6).
- [x] Add field-level `@Audited(withModifiedFlag = true)` on `SellerTerminalJpaEntity` for control/financial fields only (no class-level `@Audited`).

## 4. Firebase technical user provisioning

- [ ] Define terminal technical user email/identifier convention.
- [ ] Provision Firebase user when creating SellerTerminal.
- [ ] Update Firebase password/PIN when reset access.
- [ ] Disable Firebase user or deny through Tchalanet when disabling terminal.
- [ ] Store provider/issuer/externalSubject.
- [ ] Do not give SellerTerminal admin roles.

## 5. Queries

- [x] `ListSellerTerminalsQuery` — paged, `SellerTerminalSummaryRow` (today's sales stats null until S8).
- [x] `GetSellerTerminalQuery` — full `SellerTerminalView`.
- [x] `GetSellerTerminalForSaleValidationQuery` — minimal `SellerTerminalForSaleValidationView` with `canSell()`.
- [x] `GetSellerTerminalMeQuery` — by `SellerTerminalId` from resolved context.
- [x] Return summary rows, not persistence entities — views in `core.terminal.api.model`.

## 6. Admin controller

- [x] Add `SellerTerminalAdminController`.
- [x] Use logical path `/admin/seller-terminals`.
- [x] Use `@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN')")` on class.
- [x] Use `@RequiresPermission` on write actions (manage / block / reset_pin).
- [x] Use `@CurrentContext TchRequestContext`.
- [x] Use `CommandBus.execute` / `QueryBus.ask`.
- [x] Add `@Valid` + JSR-303 annotations on request bodies.
- [x] Add `@AuditLog` on all 6 write endpoints (7 audit events; COMMISSION_CHANGE fired implicitly via update).

## 7. Terminal endpoint

- [x] Add `GET /tenant/terminal/me` — `SellerTerminalMeController`.
- [x] Require `hasAuthority('ACTOR_SELLER_TERMINAL')`.
- [x] Returns full `SellerTerminalView` (id/code/displayName, status, commissionRate, block state, activity).

## 8. Sales integration

- [x] Update sale command to require SellerTerminal context for terminal sales.
- [x] Validate SellerTerminal exists and belongs to tenant.
- [x] Validate status `ACTIVE`.
- [x] Snapshot commission rate and amount.
- [x] Store `seller_terminal_id`.
- [x] Preserve old sales path only as legacy if needed.

## 9. Admin UI contract

- [ ] Define list DTO:
  - terminal code;
  - seller name;
  - phone;
  - status;
  - commission rate;
  - today sales;
  - today commission;
  - last sale.
- [ ] Define detail DTO:
  - identity;
  - access;
  - control profiles;
  - commission;
  - activity.
- [ ] Update naming from cashier to "Vendeurs / Terminaux".

## 10. Tests

- [ ] Create terminal.
- [ ] Duplicate terminal code rejected.
- [ ] Default commission is 15%.
- [ ] Block terminal prevents sale.
- [ ] Unblock terminal allows sale if otherwise valid.
- [ ] Disabled terminal cannot sell.
- [ ] Commission snapshot does not change after terminal commission update.
- [ ] SellerTerminal actor cannot access admin endpoints.
- [ ] Tenant admin can manage terminals with permission.
- [ ] Tenant admin without terminal permission is denied.
- [ ] Envers: `status` change recorded in `seller_terminal_aud` after block.
- [ ] Envers: `commission_rate` change recorded in `seller_terminal_aud`.
- [ ] Envers: `first_name` and `last_seen_at` absent from `seller_terminal_aud`.
- [ ] Business `audit_event` entry present for BLOCK, DISABLE, RESET_ACCESS, COMMISSION_CHANGE.
