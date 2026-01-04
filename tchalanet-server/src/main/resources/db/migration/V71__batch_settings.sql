-- V71__batch_settings.sql
-- Adds batch control flags and infra settings (idempotent)

INSERT INTO app_setting (
  level, namespace, setting_key, value_type, setting_value, active
) VALUES (
  'GLOBAL', 'batch', 'enabled', 'BOOLEAN', 'true', true
) ON CONFLICT DO NOTHING;

-- batch job switches
INSERT INTO app_setting (
  level, namespace, setting_key, value_type, setting_value, active
) VALUES
  ('GLOBAL', 'batch', 'jobs.generate.enabled', 'BOOLEAN', 'true', true),
  ('GLOBAL', 'batch', 'jobs.open.enabled', 'BOOLEAN', 'true', true),
  ('GLOBAL', 'batch', 'jobs.close.enabled', 'BOOLEAN', 'true', true),
  ('GLOBAL', 'batch', 'jobs.fetch.enabled', 'BOOLEAN', 'true', true),
  ('GLOBAL', 'batch', 'jobs.apply.enabled', 'BOOLEAN', 'true', true),
  ('GLOBAL', 'batch', 'jobs.settle.enabled', 'BOOLEAN', 'true', true)
ON CONFLICT DO NOTHING;

-- infra gate and auto-disable policy
INSERT INTO app_setting (
  level, namespace, setting_key, value_type, setting_value, active
) VALUES (
  'GLOBAL', 'batch', 'infra.enabled', 'BOOLEAN', 'true', true
) ON CONFLICT DO NOTHING;

INSERT INTO app_setting (
  level, namespace, setting_key, value_type, setting_value, active
) VALUES
  ('GLOBAL', 'batch', 'auto_disable_on_errors', 'BOOLEAN', 'true', true),
  ('GLOBAL', 'batch', 'max_consecutive_errors', 'INT', '5', true)
ON CONFLICT DO NOTHING;

