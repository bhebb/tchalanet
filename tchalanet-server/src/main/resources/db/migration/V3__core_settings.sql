-- V1003__core_settings_i18n.sql
-- Core settings & i18n overrides (requires tenant from V2)

-- =========================
-- app_setting
-- =========================
CREATE TABLE IF NOT EXISTS app_setting (
                                           id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    version bigint NOT NULL DEFAULT 0,

    level text NOT NULL,                -- GLOBAL / TENANT / OUTLET / TERMINAL
    tenant_id uuid,                     -- NULL for GLOBAL
    terminal_id uuid,
    outlet_id uuid,

    namespace text NOT NULL,
    setting_key text NOT NULL,

    value_type text NOT NULL,           -- STRING / INT / BOOLEAN / JSON
    setting_value text NOT NULL,

    active boolean NOT NULL DEFAULT true,

    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    deleted_at timestamptz,

    CONSTRAINT fk_app_setting_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenant(id),

    CONSTRAINT ck_app_setting_level
    CHECK (level IN ('GLOBAL', 'TENANT', 'OUTLET', 'TERMINAL')),

    CONSTRAINT ck_app_setting_value_type
    CHECK (value_type IN ('STRING', 'INT', 'BOOLEAN', 'JSON')),

    -- Enforce coherence between level and target ids
    CONSTRAINT ck_app_setting_target
    CHECK (
(level = 'GLOBAL'   AND tenant_id IS NULL     AND outlet_id IS NULL      AND terminal_id IS NULL)
    OR (level = 'TENANT'   AND tenant_id IS NOT NULL AND outlet_id IS NULL      AND terminal_id IS NULL)
    OR (level = 'OUTLET'   AND tenant_id IS NOT NULL AND outlet_id IS NOT NULL  AND terminal_id IS NULL)
    OR (level = 'TERMINAL' AND tenant_id IS NOT NULL AND terminal_id IS NOT NULL)
    )
    );

-- Unicity for active, non-deleted rows (avoid ambiguous overrides)
-- NOTE: Unique index (partial) allows re-creating a setting after soft-delete or deactivation.
CREATE UNIQUE INDEX IF NOT EXISTS ux_app_setting_target_key_active
    ON app_setting (level, tenant_id, terminal_id, outlet_id, namespace, setting_key)
    WHERE active = true AND deleted_at IS NULL;

-- Fast lookup for resolver (tenant/outlet/terminal + namespaces)
CREATE INDEX IF NOT EXISTS ix_app_setting_resolve_global
    ON app_setting (namespace, setting_key)
    WHERE level = 'GLOBAL' AND active = true AND deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_app_setting_resolve_tenant
    ON app_setting (tenant_id, namespace, setting_key)
    WHERE level = 'TENANT' AND active = true AND deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_app_setting_resolve_outlet
    ON app_setting (tenant_id, outlet_id, namespace, setting_key)
    WHERE level = 'OUTLET' AND active = true AND deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_app_setting_resolve_terminal
    ON app_setting (tenant_id, terminal_id, namespace, setting_key)
    WHERE level = 'TERMINAL' AND active = true AND deleted_at IS NULL;

-- =========================
-- i18n_override
-- =========================
CREATE TABLE IF NOT EXISTS i18n_override (
                                             id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    version bigint NOT NULL DEFAULT 0,

    tenant_id uuid NOT NULL REFERENCES tenant(id),
    locale text NOT NULL,               -- fr / en / ht (tu peux durcir plus tard)
    i18n_key text NOT NULL,
    i18n_value text NOT NULL,

    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    deleted_at timestamptz
    );

-- Unique per tenant+locale+key for non-deleted rows
CREATE UNIQUE INDEX IF NOT EXISTS ux_i18n_override_tenant_locale_key
    ON i18n_override (tenant_id, locale, i18n_key)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_i18n_override_lookup
    ON i18n_override (tenant_id, locale)
    WHERE deleted_at IS NULL;


-- =========================
-- triggers updated_at
-- =========================
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_app_setting_updated_at') THEN
CREATE TRIGGER trg_app_setting_updated_at
    BEFORE UPDATE ON app_setting
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_i18n_override_updated_at') THEN
CREATE TRIGGER trg_i18n_override_updated_at
    BEFORE UPDATE ON i18n_override
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
END IF;
END $$;
