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
  active,
  is_default,
  limits_json,
  features_json
)
VALUES
  -- STARTER: default platform plan
  (
    '00000000-0000-0000-0000-000000000201',
    'STARTER',
    'Starter',
    'Découverte: 1 PDV, 3 terminaux, vente manuelle uniquement',
    0.00,
    'USD',
    'MONTHLY',
    true,
    true,  -- is_default: platform default
    '{
      "outlets": 1,
      "terminals": 3,
      "users": 5,
      "maxTicketsPerDay": 200
    }'::jsonb,
    '{
      "sale.manual": true,
      "sale.offline": false,
      "promotion.basic": false,
      "payout.auto_approve": false
    }'::jsonb
  ),

  -- STANDARD: standard paid plan
  (
    '00000000-0000-0000-0000-000000000202',
    'STANDARD',
    'Standard',
    'Pour un opérateur: 5 PDV, 25 terminaux, vente offline incluse',
    99.00,
    'USD',
    'MONTHLY',
    true,
    false,
    '{
      "outlets": 5,
      "terminals": 25,
      "users": 30,
      "maxTicketsPerDay": 5000
    }'::jsonb,
    '{
      "sale.manual": true,
      "sale.offline": true,
      "promotion.basic": true,
      "payout.auto_approve": false
    }'::jsonb
  ),

  -- PRO: advanced plan
  (
    '00000000-0000-0000-0000-000000000203',
    'PRO',
    'Pro',
    'Multi-PDV, terminaux illimités, workflows avancés',
    299.00,
    'USD',
    'MONTHLY',
    true,
    false,
    '{
      "outlets": 25,
      "terminals": 500,
      "users": 200,
      "maxTicketsPerDay": 100000
    }'::jsonb,
    '{
      "sale.manual": true,
      "sale.offline": true,
      "promotion.basic": true,
      "payout.auto_approve": true
    }'::jsonb
  ),

  -- DEMO: full feature plan for trials
  (
    '00000000-0000-0000-0000-000000000204',
    'DEMO',
    'Demo / Trial',
    'Accès complet à toutes les fonctionnalités pour évaluation',
    0.00,
    'USD',
    'MONTHLY',
    true,
    false,
    '{
      "outlets": 999,
      "terminals": 9999,
      "users": 9999,
      "maxTicketsPerDay": 999999
    }'::jsonb,
    '{
      "sale.manual": true,
      "sale.offline": true,
      "promotion.basic": true,
      "payout.auto_approve": true
    }'::jsonb
  )
ON CONFLICT (code) DO UPDATE SET
  name = EXCLUDED.name,
  description = EXCLUDED.description,
  price_amount = EXCLUDED.price_amount,
  currency = EXCLUDED.currency,
  billing_period = EXCLUDED.billing_period,
  active = EXCLUDED.active,
  is_default = EXCLUDED.is_default,
  limits_json = EXCLUDED.limits_json,
  features_json = EXCLUDED.features_json;

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


