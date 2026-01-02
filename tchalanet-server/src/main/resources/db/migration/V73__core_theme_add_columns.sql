-- Add missing columns expected by ThemeJpaEntity (safe, idempotent)
ALTER TABLE IF EXISTS theme
  ADD COLUMN IF NOT EXISTS base_preset_id varchar(128),
  ADD COLUMN IF NOT EXISTS label varchar(160),
  ADD COLUMN IF NOT EXISTS mode varchar(10),
  ADD COLUMN IF NOT EXISTS density smallint,
  ADD COLUMN IF NOT EXISTS palette_json jsonb,
  ADD COLUMN IF NOT EXISTS tokens_json jsonb,
  ADD COLUMN IF NOT EXISTS css_vars_json jsonb,
  ADD COLUMN IF NOT EXISTS status varchar(20),
  ADD COLUMN IF NOT EXISTS theme_version int4;

-- Provide safe defaults for existing rows if missing
UPDATE theme
SET base_preset_id = COALESCE(base_preset_id, ''),
    label = COALESCE(label, COALESCE(name, '')),
    mode = COALESCE(mode, 'LIGHT'),
    density = COALESCE(density, 0),
    palette_json = COALESCE(palette_json, '{}'::jsonb),
    tokens_json = COALESCE(tokens_json, '{}'::jsonb),
    css_vars_json = COALESCE(css_vars_json, '{}'::jsonb),
    status = COALESCE(status, 'DRAFT'),
    theme_version = COALESCE(theme_version, 1)
WHERE TRUE;
