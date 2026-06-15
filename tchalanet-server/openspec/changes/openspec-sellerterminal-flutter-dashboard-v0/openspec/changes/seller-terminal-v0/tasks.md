# Tasks: SellerTerminal V0

## 1. Schema

- [ ] Inventory current terminal/user/seller/cashier tables.
- [ ] Decide if `seller_terminal_external_identity` exists from access-context change.
- [ ] Create `seller_terminal` table.
- [ ] Add unique `(tenant_id, terminal_code)`.
- [ ] Add unique external identity constraint or external identity table.
- [ ] Add indexes:
  - `(tenant_id, status)`
  - `(tenant_id, terminal_code)`
  - `(tenant_id, display_name)`
- [ ] Add ticket columns:
  - `seller_terminal_id`
  - `seller_commission_rate_snapshot`
  - `seller_commission_amount_snapshot`
- [ ] Keep `outlet_id` nullable.
- [ ] Keep sales session out of V0 path.

## 2. Domain model

- [ ] Add `SellerTerminalId` typed ID if missing.
- [ ] Add `SellerTerminalStatus`.
- [ ] Add `CommissionRate` value object or validate decimal range.
- [ ] Add SellerTerminal domain model.
- [ ] Implement status transitions:
  - create pending/active;
  - block;
  - unblock;
  - disable;
  - reset access metadata.
- [ ] Enforce:
  - disabled cannot become active without explicit command;
  - blocked cannot sell;
  - commission rate within accepted range.

## 3. Commands

- [ ] `CreateSellerTerminalCommand`.
- [ ] `UpdateSellerTerminalCommand`.
- [ ] `BlockSellerTerminalCommand`.
- [ ] `UnblockSellerTerminalCommand`.
- [ ] `DisableSellerTerminalCommand`.
- [ ] `ResetSellerTerminalAccessCommand`.
- [ ] Add handlers with `@UseCase`.
- [ ] Add `@TchTx` to write handlers.
- [ ] Add audit declarations.

## 4. Firebase technical user provisioning

- [ ] Define terminal technical user email/identifier convention.
- [ ] Provision Firebase user when creating SellerTerminal.
- [ ] Update Firebase password/PIN when reset access.
- [ ] Disable Firebase user or deny through Tchalanet when disabling terminal.
- [ ] Store provider/issuer/externalSubject.
- [ ] Do not give SellerTerminal admin roles.

## 5. Queries

- [ ] `ListSellerTerminalsQuery`.
- [ ] `GetSellerTerminalQuery`.
- [ ] `GetSellerTerminalForSaleValidationQuery`.
- [ ] `GetSellerTerminalMeQuery`.
- [ ] Return summary rows, not persistence entities.

## 6. Admin controller

- [ ] Add `SellerTerminalAdminController`.
- [ ] Use logical path `/admin/seller-terminals`.
- [ ] Use `@PreAuthorize` role checks on class.
- [ ] Use permission checks on actions.
- [ ] Use `@CurrentContext TchRequestContext`.
- [ ] Use `CommandBus.execute` / `QueryBus.ask`.
- [ ] Add validation annotations.
- [ ] Add audit on writes.

## 7. Terminal endpoint

- [ ] Add `/tenant/terminal/me`.
- [ ] Require `ACTOR_SELLER_TERMINAL`.
- [ ] Return:
  - terminal id/code/displayName;
  - status;
  - commission rate;
  - tenant summary;
  - allowed actions.

## 8. Sales integration

- [ ] Update sale command to require SellerTerminal context for terminal sales.
- [ ] Validate SellerTerminal exists and belongs to tenant.
- [ ] Validate status `ACTIVE`.
- [ ] Resolve odds profile from terminal or tenant default.
- [ ] Resolve limit profile from terminal or tenant default.
- [ ] Snapshot commission rate and amount.
- [ ] Store `seller_terminal_id`.
- [ ] Preserve old sales path only as legacy if needed.

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
