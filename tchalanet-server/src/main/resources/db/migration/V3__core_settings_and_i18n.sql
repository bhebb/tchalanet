-- V3: core settings & i18n (requires tenant from V2)

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
    deleted_at timestamptz
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_app_setting_target_key
    ON app_setting (level, tenant_id, terminal_id, outlet_id, namespace, setting_key)
    WHERE active = true;

CREATE TABLE IF NOT EXISTS i18n_override (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    version bigint NOT NULL DEFAULT 0,

    tenant_id uuid NOT NULL REFERENCES tenant(id),
    locale text NOT NULL,
    i18n_key text NOT NULL,
    i18n_value text NOT NULL,

    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    deleted_at timestamptz
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_i18n_override_tenant_locale_key
    ON i18n_override (tenant_id, locale, i18n_key);

-- triggers updated_at
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

