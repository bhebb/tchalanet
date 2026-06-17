# Tasks: Auditing Envers Mapping V0

## 1. Configuration

- [ ] Confirm `hibernate-envers` dependency present in `pom.xml`.
- [ ] Confirm `revinfo` table exists or is auto-created by Envers.
- [ ] Set `withModifiedFlag` globally or per field as needed.

## 2. seller_terminal

- [ ] Add field-level `@Audited(withModifiedFlag = true)` on control/financial fields only (see design.md).
- [ ] Confirm `first_name`, `last_name`, `phone_number`, `address_*`, `last_seen_at`, external identity fields have NO `@Audited`.

## 3. odds_profile / odds_rule

- [ ] `odds_profile`: partial Envers on `code`, `name`, `status`, `is_default`.
- [ ] `odds_rule`: full Envers on all business fields (exclude timestamps).

## 4. limit_profile / limit_rule

- [ ] `limit_profile`: partial Envers on `code`, `name`, `status`, `is_default`.
- [ ] `limit_rule`: full Envers on all business fields (exclude timestamps).

## 5. manual_draw_result

- [ ] Full Envers on result, status, confirmed, correction fields (exclude timestamps).

## 6. tenant_sales_policy

- [ ] Envers on commission rate, profile references, and sale/payout flags (exclude timestamps).

## 7. Verify exclusions

- [ ] `ticket` — no `@Audited` anywhere on entity or fields.
- [ ] `ticket_line` — no `@Audited`.
- [ ] `payout` — no `@Audited`.
- [ ] `audit_log` — no `@Audited`.
- [ ] `app_user` — no `@Audited`.

## 8. Tests

- [ ] `seller_terminal_aud` has entries after block and commission change.
- [ ] `seller_terminal_aud` does not have columns for `first_name`, `last_seen_at`.
- [ ] `odds_rule_aud` has entries after multiplier change.
- [ ] No `ticket_aud`, `ticket_line_aud`, `payout_aud` tables exist.
