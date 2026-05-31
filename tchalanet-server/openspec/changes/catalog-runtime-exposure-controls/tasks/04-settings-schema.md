# Task 04 — settings schema (V100)

## Goal

Add `exposure` to `app_setting` in `V100__create_core_tables.sql` (edit in place — pre-go-live).

## Steps

1. Add `exposure varchar(50) NOT NULL DEFAULT 'INTERNAL'` to the `CREATE TABLE app_setting` statement.

2. Add a CHECK constraint enumerating all valid `SettingExposure` values:

```sql
CONSTRAINT chk_app_setting__exposure CHECK (
  exposure IN ('INTERNAL','PUBLIC_RUNTIME','TENANT_RUNTIME','ADMIN_RUNTIME')
)
```

3. Remove `'GLOBAL'` from the existing `chk_app_setting__level` CHECK — `app_setting` extends `BaseTenantEntity` (non-null `tenant_id`), so `GLOBAL` level is structurally impossible. Updated:

```sql
CONSTRAINT chk_app_setting__level CHECK (
  level IN ('TENANT','OUTLET','TERMINAL')
)
```

4. Add runtime lookup index:

```sql
CREATE INDEX idx_app_setting_runtime_lookup
ON app_setting (tenant_id, exposure, active, namespace, setting_key)
WHERE deleted_at IS NULL;
```

5. Update `V101__create_audit_tables.sql` audit table for `app_setting_aud` to include `exposure` if it mirrors the main table.

## Acceptance criteria

- `app_setting.exposure` exists, is NOT NULL, defaults to `'INTERNAL'`.
- CHECK constraint rejects unknown exposure values.
- `GLOBAL` removed from level CHECK (confirm no seed data uses it first).
- `./mvnw compile -pl tchalanet-app -am -q` clean.
