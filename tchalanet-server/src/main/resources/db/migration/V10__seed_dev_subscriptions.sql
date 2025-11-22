-- Seed subscription pour tenant de développement
-- V10__seed_dev_subscriptions.sql

-- Créer une subscription active pour le tenant de dev avec le plan BASIC
INSERT INTO subscriptions (
    tenant_id,
    plan_id,
    status,
    billing_provider,
    billing_external_id,
    current_period_start,
    current_period_end,
    cancel_at_period_end,
    meta
)
SELECT
    'tenant-dev-1'::varchar,
    p.id,
    'ACTIVE'::varchar,
    'NONE'::varchar,
    'noop-tenant-dev-1'::varchar,
    NOW(),
    NOW() + INTERVAL '30 days',
    FALSE,
    '{}'::jsonb
FROM plans p
WHERE p.code = 'BASIC'
ON CONFLICT DO NOTHING;

-- Ajouter d'autres tenants de test si nécessaire
-- Exemple : tenant de test avec PRO
INSERT INTO subscriptions (
    tenant_id,
    plan_id,
    status,
    billing_provider,
    billing_external_id,
    current_period_start,
    current_period_end,
    cancel_at_period_end,
    meta
)
SELECT
    'tenant-test-1'::varchar,
    p.id,
    'ACTIVE'::varchar,
    'NONE'::varchar,
    'noop-tenant-test-1'::varchar,
    NOW(),
    NOW() + INTERVAL '30 days',
    FALSE,
    '{}'::jsonb
FROM plans p
WHERE p.code = 'PRO'
ON CONFLICT DO NOTHING;

