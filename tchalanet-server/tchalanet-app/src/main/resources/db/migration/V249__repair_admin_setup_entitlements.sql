-- V249: Repair entitlement data for tenant-admin setup surfaces on already migrated databases.
-- V201 used ON CONFLICT DO NOTHING, so existing billing_plan rows can miss newer feature keys.

UPDATE billing_plan
SET
  features_json = coalesce(features_json, '{}'::jsonb) || '{
    "tenantgame.management": true,
    "tenantgame.settings": true,
    "tenantgame.availability": true,
    "limitpolicy.basic": true,
    "promotion.campaigns.config": true,
    "promotion.rules.basic": true,
    "promotion.free_game": true,
    "theme.preset_selection": true
  }'::jsonb,
  updated_at = now()
WHERE code IN ('STARTER', 'STANDARD', 'PRO', 'DEMO')
  AND deleted_at IS NULL;

-- Local/dev and older provisioned tenants may predate subscription provisioning.
-- Without a current subscription, entitlement snapshots expose no plan features.
INSERT INTO tenant_subscription (
  id,
  tenant_id,
  plan_code,
  status,
  started_at,
  metadata_json,
  created_at,
  updated_at
)
SELECT
  gen_random_uuid(),
  t.id,
  p.code,
  'ACTIVE',
  now(),
  jsonb_build_object('source', 'V249__repair_admin_setup_entitlements'),
  now(),
  now()
FROM tenant t
CROSS JOIN LATERAL (
  SELECT code
  FROM billing_plan
  WHERE is_default = true
    AND active = true
    AND deleted_at IS NULL
  ORDER BY code
  LIMIT 1
) p
WHERE t.deleted_at IS NULL
  AND t.status = 'ACTIVE'
  AND NOT EXISTS (
    SELECT 1
    FROM tenant_subscription s
    WHERE s.tenant_id = t.id
      AND s.deleted_at IS NULL
      AND s.status IN ('ACTIVE', 'TRIAL')
      AND (s.ends_at IS NULL OR s.ends_at > now())
  );

DO $$ BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM billing_plan
    WHERE code = 'STARTER'
      AND features_json ->> 'promotion.campaigns.config' = 'true'
      AND features_json ->> 'promotion.free_game' = 'true'
      AND features_json ->> 'theme.preset_selection' = 'true'
      AND deleted_at IS NULL
  ) THEN
    RAISE EXCEPTION 'V249 sanity: STARTER admin setup entitlements missing';
  END IF;

  RAISE NOTICE 'V249 OK: admin setup entitlements repaired';
END $$;
