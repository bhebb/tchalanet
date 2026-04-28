-- Seed theme presets
INSERT INTO theme_preset (id, code, vendor, config, label_key, active, is_default)
VALUES
  ('00000000-0000-0000-0000-000000000401'::uuid, 'default-light', 'system', '{"basePresetId":"default-light","cssVars":{},"mode":"LIGHT","tokens":{}}', 'theme.default.light', true, true),
  ('00000000-0000-0000-0000-000000000402'::uuid, 'default-dark', 'system', '{"basePresetId":"default-dark","cssVars":{},"mode":"DARK","tokens":{}}', 'theme.default.dark', true, false)
ON CONFLICT (code) DO UPDATE SET
  config = EXCLUDED.config,
  label_key = EXCLUDED.label_key,
  active = EXCLUDED.active,
  is_default = EXCLUDED.is_default,
  updated_at = now();

