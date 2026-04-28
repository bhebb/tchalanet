-- Fix P0-01: add missing 'status' column to tenant_user (present in JPA, absent in V6)
ALTER TABLE tenant_user
    ADD COLUMN IF NOT EXISTS status varchar(32)
        NOT NULL DEFAULT 'ACTIVE'
        CONSTRAINT ck_tenant_user_status
            CHECK (status IN ('ACTIVE', 'SUSPENDED'));

-- Fix P1-03: align tenant_user_aud with the actual tenant_user table
--   - Add missing columns: outlet_id, terminal_id, status
--   - Remove stale column: autonomy_level (removed from entity but kept in V43 _aud)
ALTER TABLE tenant_user_aud
    ADD COLUMN IF NOT EXISTS outlet_id   uuid,
    ADD COLUMN IF NOT EXISTS terminal_id uuid,
    ADD COLUMN IF NOT EXISTS status      varchar(32);

ALTER TABLE tenant_user_aud
    DROP COLUMN IF EXISTS autonomy_level;
