# Tasks

## 1. Inventory existing DB/runtime artifacts

- [ ] List existing Flyway files that create/alter:
  - outlet
  - terminal
  - sales_session
  - ticket
  - ticket_line
  - payout
  - limitpolicy
  - autonomy
- [ ] List existing JPA entities/repositories for the same tables.
- [ ] List controllers/commands/queries that will break with the reset.
- [ ] Mark obsolete artifacts for deletion.

## 2. Recreate canonical tables

- [ ] Replace current runtime Flyway script with canonical table creation.
- [ ] Keep `tenant_id` on all tenant-scoped tables.
- [ ] Add `created_at`, `created_by`, `updated_at`, `updated_by`, `deleted_at`, `version`.
- [ ] Add RLS policies for tenant-scoped tables.
- [ ] Add indexes for common read endpoints.

## 3. Outlet schema

- [ ] Create/replace `outlet`.
- [ ] Add `state` or explicit booleans:
  - `day_closed`
  - `sales_blocked`
  - `sales_block_reason`
  - `sales_blocked_at`
- [ ] Add operational settings:
  - `timezone`
  - `business_day_cutoff`
  - `receipt_printing_enabled`
  - `receipt_header_message`
  - `receipt_footer_message`
  - `require_opening_float`
  - `auto_open_session`
  - `auto_close_session`
- [ ] Keep `address_id`.
- [ ] Unique: `(tenant_id, slug)`.
- [ ] Add indexes: `(tenant_id, deleted_at)`, `(tenant_id, sales_blocked)`.

## 4. Outlet users

- [ ] Create `outlet_user_assignment`.
- [ ] Columns:
  - `tenant_id`
  - `outlet_id`
  - `user_id`
  - `role_at_outlet`
  - `active`
  - audit columns
- [ ] Unique live assignment: `(tenant_id, outlet_id, user_id)`.

## 5. Terminal schema

- [ ] Create/replace `terminal`.
- [ ] Columns:
  - `tenant_id`
  - `outlet_id`
  - `assigned_user_id` nullable
  - `kind` = `PHYSICAL | VIRTUAL`
  - `state` = `REGISTERED | ACTIVE | LOCKED | OFFLINE | UNREGISTERED`
  - `active_for_user` boolean
  - `last_seen`
  - `label`
  - `inventory_tag`
  - `metadata` jsonb
  - `sync_state` = `ONLINE | OFFLINE | SYNC_PENDING | SYNC_CONFLICT`
  - `registered_at`, `unregistered_at`
  - `locked_at`, `locked_by`, `lock_reason`
  - audit columns
- [ ] Add unique partial index for one active terminal per user:
  - `(tenant_id, assigned_user_id) where active_for_user = true and deleted_at is null`
- [ ] Add indexes:
  - `(tenant_id, outlet_id)`
  - `(tenant_id, assigned_user_id)`
  - `(tenant_id, state)`
  - `(tenant_id, sync_state)`

## 6. Sales session schema

- [ ] Create/replace `sales_session`.
- [ ] Decision: session is seller-scoped.
- [ ] Columns:
  - `tenant_id`
  - `outlet_id`
  - `terminal_id` nullable or required at open depending MVP implementation
  - `user_id` not null
  - `status` = `OPEN | CLOSED | RECONCILED`
  - `source` = `MANUAL | SCHEDULER | OPS`
  - `opened_at`, `closed_at`
  - `opening_float`
  - `closing_amount`
  - `meta` jsonb
  - audit columns
- [ ] Add unique partial index:
  - one OPEN session per `(tenant_id, user_id)`
- [ ] Add optional unique partial index:
  - one OPEN session per `(tenant_id, terminal_id)` if terminal is required.

## 7. Sales session totals schema

- [ ] Create/replace `sales_session_totals`.
- [ ] Treat as projection/read model.
- [ ] Columns:
  - `tenant_id`
  - `session_id`
  - `total_tickets`
  - `total_stake_cents`
  - `total_payout_cents`
  - `gross_margin_cents`
  - `last_recomputed_at`
  - audit columns
- [ ] Unique: `(session_id)`.

## 8. Ticket schema

- [ ] Create/replace `ticket`.
- [ ] Columns:
  - `tenant_id`
  - `outlet_id`
  - `terminal_id`
  - `draw_id`
  - `session_id` NOT NULL
  - `user_id` NOT NULL seller snapshot
  - `ticket_code` NOT NULL
  - `public_code` NOT NULL
  - `sale_status`
  - `result_status`
  - `settlement_status`
  - `currency`
  - `total_amount_cents`
  - `winning_amount_cents`
  - `resulted_at`
  - `approval_request_id`
  - audit columns
- [ ] Unique:
  - `(tenant_id, ticket_code)`
  - `(public_code)`
- [ ] Indexes:
  - `(tenant_id, session_id)`
  - `(tenant_id, outlet_id, created_at)`
  - `(tenant_id, user_id, created_at)`
  - `(tenant_id, draw_id)`
  - `(tenant_id, sale_status, result_status, settlement_status)`

## 9. Ticket line schema

- [ ] Create/replace `ticket_line`.
- [ ] Add `tenant_id` for direct RLS and easier queries.
- [ ] Columns:
  - `tenant_id`
  - `ticket_id`
  - `game_code`
  - `selection`
  - `stake_cents`
  - `odds_snapshot`
  - `potential_payout_cents`
  - `bet_type`
  - `bet_option`
  - audit columns
- [ ] Indexes:
  - `(tenant_id, ticket_id)`
  - `(tenant_id, game_code)`

## 10. Payout schema

- [ ] Create/replace `payout`.
- [ ] Columns:
  - `tenant_id`
  - `ticket_id`
  - `selling_outlet_id`
  - `selling_session_id`
  - `paying_outlet_id`
  - `paying_session_id` nullable
  - `terminal_id` nullable
  - `paid_by_user_id`
  - `amount_cents`
  - `currency`
  - `status` = `REQUESTED | APPROVED | PAID | REJECTED | CANCELLED`
  - `approved_at`, `paid_at`, `rejected_at`
  - `rejected_reason`
  - audit columns
- [ ] Add unique constraint to prevent double paid payout for same ticket if applicable.
- [ ] Add indexes by status/date/outlet/session.

## 11. Limit policy schema

- [ ] Create `limit_definition`.
- [ ] Global/system table or tenant-independent system definitions:
  - `rule_key`
  - `enabled`
  - `on_breach`
  - `params`
  - `applies_to`
- [ ] Create `limit_assignment`.
- [ ] Tenant-scoped assignments:
  - `tenant_id`
  - `limit_definition_id`
  - `target_type` = `TENANT | OUTLET | USER`
  - `target_id` nullable for TENANT
  - `enabled`
  - `starts_at`
  - `ends_at`
  - `params_override`
  - `applies_to_override`
- [ ] Remove TERMINAL target from MVP.
- [ ] Add natural unique constraints.

## 12. Autonomy schema

- [ ] Create `autonomy_policy_rule`.
- [ ] Columns:
  - `tenant_id`
  - `target_type` = `TENANT | OUTLET | USER`
  - `target_id` nullable for TENANT
  - `level`
  - `require_approval_on_block`
  - `approval_role`
  - `enabled`
  - `starts_at`, `ends_at`
  - audit columns
- [ ] Add natural unique constraints for active target.

## 13. Approval request schema

- [ ] Create `approval_request`.
- [ ] Columns:
  - `tenant_id`
  - `entity_type`
  - `entity_id`
  - `reason_code`
  - `status` = `PENDING | APPROVED | REJECTED | EXPIRED`
  - `requested_by`
  - `approved_by`
  - `rejected_by`
  - `requested_at`, `decided_at`
  - `details` jsonb
- [ ] Used by sales and payout when limit/autonomy requires approval.

## 14. Validation

- [ ] Run Flyway clean/migrate on empty DB.
- [ ] Run `ddl-auto=validate`.
- [ ] Verify RLS policies.
- [ ] Verify Envers/AUD strategy if enabled now.
- [ ] Update seed data.
