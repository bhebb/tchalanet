-- V31: seed billing_plan (FREE / BASIC / ENTERPRISE)
-- Conformance: catalog/plan spec (P1-P5)
-- Table: billing_plan (renamed from 'plan' for clarity)

DO $$ BEGIN
  RAISE NOTICE 'V31__seed_plans: seeding billing_plan (FREE/BASIC/ENTERPRISE)';
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
  -- FREE: default platform plan
  (
    '00000000-0000-0000-0000-000000000201',
    'free',
    'Free',
    'Découverte: pages publiques + résultats + vérification de ticket',
    0.00,
    'USD',
    'MONTHLY',
    true,
    true,  -- is_default: platform default
    '{
      "outlets": 1,
      "cashiers": 2,
      "maxTicketsPerDay": 100
    }'::jsonb,
    '[
      "Page publique (Home + news + résultats)",
      "Vérifier un ticket (/verifier + /ticket/:code)",
      "Résultats US (NY/FL) en lecture",
      "Support de base (email)"
    ]'::jsonb
  ),

  -- BASIC: standard paid plan
  (
    '00000000-0000-0000-0000-000000000202',
    'basic',
    'Basic',
    'Pour un opérateur: dashboards + POS web + rapports simples',
    99.00,
    'USD',
    'MONTHLY',
    true,
    false,
    '{
      "outlets": 5,
      "cashiers": 25,
      "maxTicketsPerDay": 5000
    }'::jsonb,
    '[
      "Dashboard privé (vendeur/opérateur/admin)",
      "POS Web (vente + sessions) - v1",
      "Gestion jeux par tenant (activer/désactiver canaux)",
      "Rapports simples (ventes/jour, par PDV, par caissier)",
      "Support prioritaire"
    ]'::jsonb
  ),

  -- ENTERPRISE: advanced plan
  (
    '00000000-0000-0000-0000-000000000203',
    'enterprise',
    'Enterprise',
    'Multi-PDV, conformité, options avancées (sur devis)',
    299.00,
    'USD',
    'MONTHLY',
    true,
    false,
    '{
      "outlets": 999,
      "cashiers": 9999,
      "maxTicketsPerDay": 999999
    }'::jsonb,
    '[
      "Multi-PDV + workflows (autonomy policy)",
      "Audit & conformité renforcés",
      "Exports & intégrations (API/BI) - v2",
      "SLA + support dédié"
    ]'::jsonb
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

-- Sanity check: ensure all 3 plans exist and exactly one is default
DO $$
DECLARE
  plan_count int;
  default_count int;
BEGIN
  SELECT count(*) INTO plan_count
  FROM billing_plan
  WHERE code IN ('free', 'basic', 'enterprise')
    AND deleted_at IS NULL;

  SELECT count(*) INTO default_count
  FROM billing_plan
  WHERE is_default = true
    AND active = true
    AND deleted_at IS NULL;

  IF plan_count < 3 THEN
    RAISE EXCEPTION 'V31__seed_plans sanity check failed: expected 3 plans, found %', plan_count;
  END IF;

  IF default_count != 1 THEN
    RAISE EXCEPTION 'V31__seed_plans sanity check failed: expected exactly 1 default plan, found %', default_count;
  END IF;

  RAISE NOTICE 'V31__seed_plans sanity check OK: % plans present, % default plan', plan_count, default_count;
END $$;

