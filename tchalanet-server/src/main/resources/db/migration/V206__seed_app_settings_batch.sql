-- pour debloquer

CREATE POLICY app_setting_platform_global_all ON app_setting
FOR ALL
USING (
  level = 'GLOBAL'
  AND tenant_id IS NULL
  AND public.allow_platform_cross_tenant_select()
)
WITH CHECK (
  level = 'GLOBAL'
  AND tenant_id IS NULL
  AND public.allow_platform_cross_tenant_select()
);

CREATE POLICY app_setting_tenant_all ON app_setting
FOR ALL
USING (
  tenant_id = public.current_tenant()
  AND public.current_tenant() IS NOT NULL
)
WITH CHECK (
  tenant_id = public.current_tenant()
  AND public.current_tenant() IS NOT NULL
);

-- V1003__seed_batch_settings.sql
-- Move seeds for batch settings separate from structural migration (idempotent)
-- V1003__seed_batch_settings.sql

SELECT set_config('app.current_tenant', '', true);
SELECT set_config('app.deleted_visibility', 'active', true);
SELECT set_config('app.api_scope', 'platform', true);
SELECT set_config('app.is_super_admin', 'true', true);

INSERT INTO app_setting (
    level, namespace, setting_key, value_type, setting_value, active
)
VALUES
    ('GLOBAL', 'batch', 'enabled', 'BOOLEAN', 'true', true),
    ('GLOBAL', 'batch', 'jobs.generate.enabled', 'BOOLEAN', 'true', true),
    ('GLOBAL', 'batch', 'jobs.open.enabled', 'BOOLEAN', 'true', true),
    ('GLOBAL', 'batch', 'jobs.close.enabled', 'BOOLEAN', 'true', true),
    ('GLOBAL', 'batch', 'jobs.fetch.enabled', 'BOOLEAN', 'true', true),
    ('GLOBAL', 'batch', 'jobs.apply.enabled', 'BOOLEAN', 'true', true),
    ('GLOBAL', 'batch', 'jobs.settle.enabled', 'BOOLEAN', 'true', true),
    ('GLOBAL', 'batch', 'infra.enabled', 'BOOLEAN', 'true', true),
    ('GLOBAL', 'batch', 'auto_disable_on_errors', 'BOOLEAN', 'true', true),
    ('GLOBAL', 'batch', 'max_consecutive_errors', 'INT', '5', true)
    ON CONFLICT DO NOTHING;

SELECT set_config('app.current_tenant', '', true);
SELECT set_config('app.deleted_visibility', 'active', true);
SELECT set_config('app.api_scope', '', true);
SELECT set_config('app.is_super_admin', 'false', true);
