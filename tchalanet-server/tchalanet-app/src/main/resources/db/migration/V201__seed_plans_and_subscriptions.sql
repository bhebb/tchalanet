-- V201: seed billing_plan (STARTER, STANDARD, PRO, DEMO)
-- Conformance: catalog/plan spec (P1-P5)
-- Table: billing_plan (renamed from 'plan' for clarity)

DO $$ BEGIN
  RAISE NOTICE 'V201__seed_plans: seeding billing_plan (STARTER/STANDARD/PRO/DEMO)';
END $$;

INSERT INTO billing_plan (
  id,
  code,
  name,
  description,
  price_amount,
  currency,
  billing_period,
  features_json,
  limits_json,
  active,
  is_default,
  created_at,
  updated_at
)
VALUES (
  gen_random_uuid(),
  'STARTER',
  'Starter',
  'Petit tenant avec opérations de base.',
  0,
  'USD',
  'MONTHLY',
  '{
    "tenantgame.management": true,
    "promotion.campaigns.config": true
  }'::jsonb,
  '{
    "limits.admin_users.max": 3,
    "limits.seller_terminals.max": 10,
    "limits.draw_channels.max": 3,
    "limits.promotion_rules.max": 5
  }'::jsonb,
  true,
  true,
  now(),
  now()
)
ON CONFLICT (code) DO NOTHING;

INSERT INTO billing_plan (
  id,
  code,
  name,
  description,
  price_amount,
  currency,
  billing_period,
  features_json,
  limits_json,
  active,
  is_default,
  created_at,
  updated_at
)
VALUES (
  gen_random_uuid(),
  'STANDARD',
  'Standard',
  'Tenant structuré avec plusieurs admins et seller terminals.',
  0,
  'USD',
  'MONTHLY',
  '{
    "tenantgame.management": true,
    "promotion.campaigns.config": true,
    "theme.preset_selection": true
  }'::jsonb,
  '{
    "limits.admin_users.max": 8,
    "limits.seller_terminals.max": 30,
    "limits.draw_channels.max": 8,
    "limits.promotion_rules.max": 5
  }'::jsonb,
  true,
  false,
  now(),
  now()
)
ON CONFLICT (code) DO NOTHING;

INSERT INTO billing_plan (
  id,
  code,
  name,
  description,
  price_amount,
  currency,
  billing_period,
  features_json,
  limits_json,
  active,
  is_default,
  created_at,
  updated_at
)
VALUES (
  gen_random_uuid(),
  'PRO',
  'Pro',
  'Tenant avancé avec capacité élevée, promotions et personnalisation du thème.',
  0,
  'USD',
  'MONTHLY',
  '{
    "tenantgame.management": true,
    "promotion.campaigns.config": true,
    "theme.preset_selection": true
  }'::jsonb,
  '{
    "limits.admin_users.max": 20,
    "limits.seller_terminals.max": 100,
    "limits.draw_channels.max": 20,
    "limits.promotion_rules.max": 20
  }'::jsonb,
  true,
  false,
  now(),
  now()
)
ON CONFLICT (code) DO NOTHING;

INSERT INTO billing_plan (
  id,
  code,
  name,
  description,
  price_amount,
  currency,
  billing_period,
  features_json,
  limits_json,
  active,
  is_default,
  created_at,
  updated_at
)
VALUES (
  gen_random_uuid(),
  'DEMO',
  'Demo / Trial',
  'Accès complet à toutes les fonctionnalités pour évaluation',
  0.00,
  'USD',
  'MONTHLY',
  '{
      "tenantgame.management": true,
      "promotion.campaigns.config": true,
      "theme.preset_selection": true
  }'::jsonb,
  '{
      "limits.admin_users.max": 9999,
      "limits.seller_terminals.max": 9999,
      "limits.draw_channels.max": 999,
      "limits.promotion_rules.max": 999
  }'::jsonb,
  true,
  false,
  now(),
  now()
)
ON CONFLICT (code) DO NOTHING;

-- Sanity check: ensure all 4 plans exist and exactly one is default
DO $$
DECLARE
  plan_count int;
  default_count int;
BEGIN
  SELECT count(*) INTO plan_count
  FROM billing_plan
  WHERE code IN ('STARTER', 'STANDARD', 'PRO', 'DEMO')
    AND deleted_at IS NULL;

  SELECT count(*) INTO default_count
  FROM billing_plan
  WHERE is_default = true
    AND active = true
    AND deleted_at IS NULL;

  IF plan_count < 4 THEN
    RAISE EXCEPTION 'V201__seed_plans sanity check failed: expected 4 plans, found %', plan_count;
  END IF;

  IF default_count != 1 THEN
    RAISE EXCEPTION 'V201__seed_plans sanity check failed: expected exactly 1 default plan, found %', default_count;
  END IF;

  RAISE NOTICE 'V201__seed_plans sanity check OK: % plans present, % default plan', plan_count, default_count;
END $$;

-- ─────────────────────────────────────────────────────────────────────────────
-- Seed a default-plan subscription for every active/draft tenant that lacks one,
-- so gated admin setup surfaces can resolve plan entitlements. Idempotent.
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO tenant_subscription (id, tenant_id, plan_code, status, started_at, metadata_json, created_at, updated_at)
SELECT gen_random_uuid(), t.id, p.code, 'ACTIVE', now(),
       jsonb_build_object('source', 'V201__seed_plans_and_subscriptions'), now(), now()
FROM tenant t
CROSS JOIN LATERAL (
  SELECT code FROM billing_plan
  WHERE is_default = true AND active = true AND deleted_at IS NULL
  ORDER BY code LIMIT 1
) p
WHERE t.deleted_at IS NULL
  AND t.status IN ('ACTIVE', 'DRAFT')
  AND NOT EXISTS (
    SELECT 1 FROM tenant_subscription s
    WHERE s.tenant_id = t.id AND s.deleted_at IS NULL
      AND s.status IN ('ACTIVE', 'TRIAL')
      AND (s.ends_at IS NULL OR s.ends_at > now())
  );
