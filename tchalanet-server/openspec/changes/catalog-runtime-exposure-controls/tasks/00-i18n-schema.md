# Task 00 — i18n schema (V100)

## Goal

Add `surface` to `i18n_override` in `V100__create_core_tables.sql` (edit in place — pre-go-live).

## Steps

1. Add `surface varchar(50) NOT NULL DEFAULT 'INTERNAL'` to the `CREATE TABLE i18n_override` statement.

2. Add a CHECK constraint enumerating all valid `I18nSurface` values:

```sql
CONSTRAINT chk_i18n_override__surface CHECK (
  surface IN (
    'PUBLIC_HOME','PUBLIC_RESULTS','PUBLIC_TICKET_CHECK','COMMON_PUBLIC_ERROR',
    'AUTH','CASHIER','TENANT_ADMIN','PLATFORM_ADMIN','COMMON_PRIVATE_ERROR','INTERNAL'
  )
)
```

3. Replace the existing unique indexes on `i18n_override` to include `surface`:

```sql
CREATE UNIQUE INDEX uq_i18n_override_global
ON i18n_override (surface, locale, i18n_key)
WHERE tenant_id IS NULL AND deleted_at IS NULL;

CREATE UNIQUE INDEX uq_i18n_override_tenant
ON i18n_override (tenant_id, surface, locale, i18n_key)
WHERE tenant_id IS NOT NULL AND deleted_at IS NULL;
```

4. Add runtime lookup index:

```sql
CREATE INDEX idx_i18n_override_runtime_lookup
ON i18n_override (tenant_id, surface, locale, active)
WHERE deleted_at IS NULL;
```

5. Update `V101__create_audit_tables.sql` audit table for `i18n_override_aud` to include `surface` if it mirrors the main table structure.

## Acceptance criteria

- `i18n_override.surface` exists, is NOT NULL, defaults to `'INTERNAL'`.
- CHECK constraint rejects unknown surface values.
- Unique indexes include `surface` — same key can exist in two different surfaces.
- `./mvnw compile -pl tchalanet-app -am -q` passes (Flyway migration parses cleanly).
