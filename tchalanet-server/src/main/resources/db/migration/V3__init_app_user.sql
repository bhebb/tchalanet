CREATE EXTENSION IF NOT EXISTS citext;

CREATE TABLE IF NOT EXISTS app_user (
                                        id                  UUID PRIMARY KEY,             -- Keycloak sub
                                        username            TEXT NOT NULL,                -- preferred_username snapshot
                                        email               CITEXT,                       -- optional snapshot
                                        tenant              TEXT NOT NULL,                -- claim "tenant"
                                        active_enterprise_id TEXT,                        -- claim "active_enterprise_id"
                                        display_name        TEXT,
                                        locale              TEXT,
                                        last_login_at       TIMESTAMPTZ,
                                        created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN NEW.updated_at = now(); RETURN NEW; END $$;

DROP TRIGGER IF EXISTS trg_app_user_updated_at ON app_user;
CREATE TRIGGER trg_app_user_updated_at
    BEFORE UPDATE ON app_user
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Helpful index if you often filter by tenant
CREATE INDEX IF NOT EXISTS idx_app_user_tenant ON app_user(tenant);
