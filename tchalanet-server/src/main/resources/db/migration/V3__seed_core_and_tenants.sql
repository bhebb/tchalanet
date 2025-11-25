-- V4__seed_core_and_themes.sql

-- ===================
-- 1. TENANTS
-- ===================
INSERT INTO tenant (id, code, name, timezone, currency, status, active_theme_id)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'platform', 'Tchalanet Platform', 'America/Toronto', 'USD', 'ACTIVE', NULL),
    ('00000000-0000-0000-0000-000000000002', 'demo',      'Tchalanet Demo',      'America/Toronto', 'USD', 'ACTIVE', NULL);


-- ===================
-- 2. GAMES
-- ===================
INSERT INTO game (code, name, category, min_digits, max_digits, combination, description, active, sort_order)
VALUES
    ('BORLETTE_3', 'Borlette 3 chiffres', 'BORLETTE', 0, 999, 'STRAIGHT',
     'Sélection de 3 chiffres (000–999), payoff typique de la borlette.', true, 10),
    ('BORLETTE_5', 'Borlette 5 chiffres', 'BORLETTE', 0, 99999, 'STRAIGHT',
     'Sélection de 5 chiffres (00000–99999), gains plus élevés.', true, 20),
    ('MARIAGE', 'Mariage', 'MARRIAGE', 1, 99, 'PAIR',
     'Mariage de deux nombres (ex: 12-34) sur un tirage donné.', true, 30),
    ('LOTTO_5_40', 'Lotto 5/40', 'LOTTO', 1, 40, 'COMBO',
     'Jeu de lotto où le joueur choisit 5 numéros parmi 40.', true, 40),
    ('LOTTO_6_50', 'Lotto 6/50', 'LOTTO', 1, 50, 'COMBO',
     'Lotto avancé: 6 numéros parmi 50.', true, 50)
    ON CONFLICT (code) DO NOTHING;

-- ===================
-- 3. TENANT_GAME
-- ===================
-- We insert tenant_game rows by joining tenants and games on their codes to ensure the game IDs exist.
-- This avoids FK violations when the game IDs are not known as literals.
INSERT INTO tenant_game (tenant_id, game_id, enabled, display_name, min_stake, max_stake)
SELECT t.id, g.id, true, 'Borlette 3', 1.00, 100.00
FROM tenant t
JOIN game g ON g.code = 'BORLETTE_3'
WHERE t.code IN ('platform', 'demo')
ON CONFLICT (tenant_id, game_id) DO NOTHING;

INSERT INTO tenant_game (tenant_id, game_id, enabled, display_name, min_stake, max_stake)
SELECT t.id, g.id, true, 'Borlette 5', 1.00, 200.00
FROM tenant t
JOIN game g ON g.code = 'BORLETTE_5'
WHERE t.code IN ('platform', 'demo')
ON CONFLICT (tenant_id, game_id) DO NOTHING;


-- ===================
-- 4. PLAN & SUBSCRIPTION
-- ===================
INSERT INTO plan (code, name, description, price_amount, currency, billing_frequency, public_plan, features)
VALUES
    (
        'BASIC',
        'Essentiel',
        'Pour démarrer avec 1 à 3 points de vente.',
        99.00,
        'USD',
        'MONTH',
        true,
        '[
          "plans.feat.outlets_3",
          "plans.feat.terminals_10",
          "plans.feat.draws_standard",
          "plans.feat.pos_web_only",
          "plans.feat.reports_basic",
          "plans.feat.support_email"
        ]'::jsonb
    ),
    (
        'PRO',
        'Pro',
        'Pour opérateurs avec réseau multi-points et besoins de rapports avancés.',
        249.00,
        'USD',
        'MONTH',
        true,
        '[
          "plans.feat.outlets_20",
          "plans.feat.terminals_80",
          "plans.feat.draws_advanced",
          "plans.feat.pos_web_mobile",
          "plans.feat.reports_advanced",
          "plans.feat.theming_custom",
          "plans.feat.limits_advanced",
          "plans.feat.support_email_chat"
        ]'::jsonb
    ),
    (
        'ENTERPRISE',
        'Entreprise',
        'Pour opérateurs nationaux avec besoins d’intégrations et SLA.',
        0.00,
        'USD',
        'MONTH',
        true,
        '[
          "plans.feat.outlets_unlimited",
          "plans.feat.terminals_unlimited",
          "plans.feat.draws_external_integration",
          "plans.feat.pos_web_mobile_offline",
          "plans.feat.reports_custom",
          "plans.feat.theming_full",
          "plans.feat.limits_risk_engine",
          "plans.feat.audit_compliance",
          "plans.feat.support_sla"
        ]'::jsonb
    )
    ON CONFLICT (code) DO NOTHING;


-- ==========================
-- 5. Subscription de démo (tenant demo sur plan PRO)
-- ===========================

INSERT INTO subscription (
    tenant_id,
    plan_id,
    status,
    current_period_start,
    current_period_end,
    cancel_at_period_end,
    billing_provider,
    billing_external_id,
    meta
)
SELECT
    t.id,
    p.id,
    'ACTIVE',
    now(),
    now() + interval '30 days',
    false,
    'NONE',
    'seed-demo-subscription',
    '{}'::jsonb
FROM tenant t
    JOIN plan p ON p.code = 'PRO'
WHERE t.code = 'demo'
ON CONFLICT DO NOTHING;


-- ==========================
-- 6. DRAW_CHANNEL pour le tenant de démo
-- ==========================

WITH demo AS (
    SELECT id AS tenant_id FROM tenant WHERE code = 'demo'
),
     cfg AS (
         SELECT *
         FROM (VALUES
                   ('NY_MID',        'New York Midday',        'BORLETTE_3', 'America/New_York',       '12:30:00'::time, 180, 'MON-SAT', true, 10),
                   ('NY_EVE',        'New York Evening',       'BORLETTE_3', 'America/New_York',       '19:30:00'::time, 180, 'MON-SAT', true, 20),
                   ('FL_MID',        'Florida Midday',         'BORLETTE_3', 'America/New_York',       '13:30:00'::time, 180, 'MON-SAT', true, 30),
                   ('FL_EVE',        'Florida Evening',        'BORLETTE_3', 'America/New_York',       '21:00:00'::time, 180, 'MON-SAT', true, 40),
                   ('HT_LOTTO_5_40', 'Lotto national 5/40',    'LOTTO_5_40', 'America/Port-au-Prince', '20:00:00'::time, 300, 'WED-SAT-SUN', true, 50)
              ) AS t(code, name, game_code, tz, draw_time, cutoff_sec, days_of_week, active, sort_order)
     )
INSERT INTO draw_channel (
  tenant_id,
  code,
  name,
  game_id,
  timezone,
  draw_time,
  cutoff_sec,
  days_of_week,
  active,
  sort_order
)
SELECT
    d.tenant_id,
    c.code,
    c.name,
    g.id             AS game_id,
    c.tz,
    c.draw_time,
    c.cutoff_sec,
    c.days_of_week,
    c.active,
    c.sort_order
FROM demo d
         JOIN cfg c ON true
         JOIN game g ON g.code = c.game_code
    ON CONFLICT (tenant_id, code) DO NOTHING;

-- ===================
-- 5. THEME PRESETS GLOBAUX
-- ===================
INSERT INTO theme (
    id, version, tenant_id, base_preset_id, label, mode, density,
    palette_json, tokens_json, css_vars_json,
    status, theme_version
) VALUES
      -- 1. Tchalanet "officiel"
      ('55500000-0000-0000-0000-000000000001', 0, NULL,
       'tchalanet',
       'Tchalanet (par défaut)',
       'system', 0,
       '{}'::jsonb, '{}'::jsonb, '{}'::jsonb,
       'published', 1),

      -- 2. Violet–Green
      ('55500000-0000-0000-0000-000000000002', 0, NULL,
       'violet_green',
       'Violet & Green',
       'system', 0,
       '{}'::jsonb, '{}'::jsonb, '{}'::jsonb,
       'published', 1),

      -- 3. Rose–Blue
      ('55500000-0000-0000-0000-000000000003', 0, NULL,
       'rose_blue',
       'Rose & Blue',
       'system', 0,
       '{}'::jsonb, '{}'::jsonb, '{}'::jsonb,
       'published', 1),

      -- 4. Red–Gold
      ('55500000-0000-0000-0000-000000000004', 0, NULL,
       'red_gold',
       'Red & Gold',
       'system', 0,
       '{}'::jsonb, '{}'::jsonb, '{}'::jsonb,
       'published', 1),

      -- 5. Indigo–Orange
      ('55500000-0000-0000-0000-000000000005', 0, NULL,
       'indigo_orange',
       'Indigo & Orange',
       'system', 0,
       '{}'::jsonb, '{}'::jsonb, '{}'::jsonb,
       'published', 1),

      -- 6. Cyan–Magenta
      ('55500000-0000-0000-0000-000000000006', 0, NULL,
       'cyan_magenta',
       'Cyan & Magenta',
       'system', 0,
       '{}'::jsonb, '{}'::jsonb, '{}'::jsonb,
       'published', 1),

      -- 7. Spring Green–Violet
      ('55500000-0000-0000-0000-000000000007', 0, NULL,
       'spring_green_violet',
       'Spring Green & Violet',
       'system', 0,
       '{}'::jsonb, '{}'::jsonb, '{}'::jsonb,
       'published', 1),

      -- 8. Azure–Rose
      ('55500000-0000-0000-0000-000000000008', 0, NULL,
       'azure_rose',
       'Azure & Rose',
       'system', 0,
       '{}'::jsonb, '{}'::jsonb, '{}'::jsonb,
       'published', 1),

      -- 9. Chartreuse–Indigo
      ('55500000-0000-0000-0000-000000000009', 0, NULL,
       'chartreuse_indigo',
       'Chartreuse & Indigo',
       'system', 0,
       '{}'::jsonb, '{}'::jsonb, '{}'::jsonb,
       'published', 1);


-- ===================
-- 6. Lier les tenants au thème "tchalanet"
-- ===================
UPDATE tenant
SET active_theme_id = '55500000-0000-0000-0000-000000000001'
WHERE id IN (
             '00000000-0000-0000-0000-000000000001', -- platform
             '00000000-0000-0000-0000-000000000002'  -- demo
    );
