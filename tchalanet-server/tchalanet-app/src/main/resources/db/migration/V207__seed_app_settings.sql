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
    level, namespace, setting_key, value_type, setting_value, active, exposure
)
VALUES
    ('GLOBAL', 'batch.gate', 'batch:global:enabled',        'BOOLEAN', 'true', true, 'INTERNAL'),
    ('GLOBAL', 'batch.gate', 'results:external:fetch',      'BOOLEAN', 'true', true, 'INTERNAL'),
    ('GLOBAL', 'batch.gate', 'results:external:apply',      'BOOLEAN', 'true', true, 'INTERNAL'),
    ('GLOBAL', 'batch.gate', 'results:external:refresh',    'BOOLEAN', 'true', true, 'INTERNAL'),
    ('GLOBAL', 'batch.gate', 'results:external:manual',     'BOOLEAN', 'true', true, 'INTERNAL'),
    ('GLOBAL', 'batch.gate', 'results:external:override',   'BOOLEAN', 'true', true, 'INTERNAL'),
    ('GLOBAL', 'batch.gate', 'draw:lifecycle:generate',     'BOOLEAN', 'true', true, 'INTERNAL'),
    ('GLOBAL', 'batch.gate', 'draw:lifecycle:open',         'BOOLEAN', 'true', true, 'INTERNAL'),
    ('GLOBAL', 'batch.gate', 'draw:lifecycle:close',        'BOOLEAN', 'true', true, 'INTERNAL'),
    ('GLOBAL', 'batch.gate', 'draw:lifecycle:settle',       'BOOLEAN', 'true', true, 'INTERNAL'),
    ('GLOBAL', 'batch.gate', 'draw:watchdog:provisional',   'BOOLEAN', 'true', true, 'INTERNAL')
    ON CONFLICT DO NOTHING;

SELECT set_config('app.current_tenant', '', true);
SELECT set_config('app.deleted_visibility', 'active', true);
SELECT set_config('app.api_scope', '', true);
SELECT set_config('app.is_super_admin', 'false', true);
