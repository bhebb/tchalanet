# Tasks: Auditing Envers Mapping V0

## 1. Configuration

- [ ] Confirm `hibernate-envers` dependency present in `pom.xml`.
- [ ] Confirm `revinfo` table exists through controlled Flyway migration.
- [ ] Set `withModifiedFlag` globally or per field as needed.

## 2. seller_terminal

- [ ] Add field-level `@Audited(withModifiedFlag = true)` on control/financial fields only (see design.md).
- [ ] Confirm `first_name`, `last_name`, `phone_number`, `address_*`, `last_seen_at`, external identity fields have NO `@Audited`.

## 3. draw_result

- [ ] `draw_result`: Envers revisions are produced by JPA writes for external fetch, manual result,
  confirmation and override paths.

## 4. limit_assignment

- [ ] `limit_assignment`: Envers revisions exist only for the allowlisted assignment entity.

## 7. Verify exclusions

- [ ] `ticket` — no `@Audited` anywhere on entity or fields.
- [ ] `ticket_line` — no `@Audited`.
- [ ] `payout` — no `@Audited`.
- [ ] `audit_event` — no `@Audited`.
- [ ] `app_user` — no `@Audited`.

## 8. Tests

- [ ] `seller_terminal_aud` has entries after block and commission change.
- [ ] `seller_terminal_aud` does not have columns for `first_name`, `last_seen_at`.
- [ ] `draw_result_aud` has entries after create/manual/override.
- [ ] `limit_assignment_aud` has entries after assignment update.
- [ ] No `ticket_aud`, `ticket_line_aud`, `payout_aud` tables exist.
