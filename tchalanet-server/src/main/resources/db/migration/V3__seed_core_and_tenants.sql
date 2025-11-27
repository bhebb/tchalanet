-- V3__seed_core_and_tenants.sql

-- ===================
-- 1. TENANTS
-- ===================
INSERT INTO tenant (id, code, name, timezone, currency, status, active_theme_id)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'platform',  'Tchalanet Platform', 'America/Toronto', 'USD', 'ACTIVE', NULL),
    ('00000000-0000-0000-0000-000000000002', 'demo',      'Tchalanet Demo',      'America/Toronto', 'USD', 'ACTIVE', NULL),
    ('00000000-0000-0000-0000-000000000003', 'tchalanet', 'Tchalanet Default',   'America/Toronto', 'USD', 'ACTIVE', NULL)
ON CONFLICT (id) DO NOTHING;


-- ===================
-- 2. GAMES (catalogue global)
-- ===================
INSERT INTO game (code, name, category, min_digits, max_digits, combination, description, active, sort_order)
VALUES
    -- Borlette / Numbers (3, 4, 5 chiffres)
    ('BORLETTE_3', 'Borlette 3 chiffres', 'BORLETTE', 0, 999, 'STRAIGHT',
     'Sélection de 3 chiffres (000–999), payoff typique de la borlette et des jeux type Pick 3 / Numbers 3.', true, 10),

    ('BORLETTE_4', 'Borlette 4 chiffres', 'BORLETTE', 0, 9999, 'STRAIGHT',
     'Sélection de 4 chiffres (0000–9999), pour des jeux type Pick 4 / Numbers 4.', true, 20),

    ('BORLETTE_5', 'Borlette 5 chiffres', 'BORLETTE', 0, 99999, 'STRAIGHT',
     'Sélection de 5 chiffres (00000–99999), gains plus élevés.', true, 30),

    -- Mariage (Haiti)
    ('MARIAGE', 'Mariage 2 chiffres', 'MARRIAGE', 1, 99, 'PAIR',
     'Mariage de deux nombres (ex: 12-34) sur un tirage donné.', true, 40),

    -- Lotto "N chiffres" (Haiti, winners = nombre à N chiffres)
    ('LOTTO_3', 'Lotto 3 chiffres', 'LOTTO', 0, 999, 'STRAIGHT',
     'Lotto 3 chiffres (000–999).', true, 50),

    ('LOTTO_4', 'Lotto 4 chiffres', 'LOTTO', 0, 9999, 'STRAIGHT',
     'Lotto 4 chiffres (0000–9999).', true, 60),

    ('LOTTO_5', 'Lotto 5 chiffres', 'LOTTO', 0, 99999, 'STRAIGHT',
     'Lotto 5 chiffres (00000–99999).', true, 70),

    -- Lotto combinatoire
    ('LOTTO_5_40', 'Lotto 5/40', 'LOTTO', 1, 40, 'COMBO',
     'Jeu de lotto où le joueur choisit 5 numéros parmi 40.', true, 80),

    ('LOTTO_6_50', 'Lotto 6/50', 'LOTTO', 1, 50, 'COMBO',
     'Lotto avancé: 6 numéros parmi 50.', true, 90)
ON CONFLICT (code) DO NOTHING;


-- ===================
-- 3. TENANT_GAME (jeux activés pour platform/demo/tchalanet)
-- ===================
WITH tg_tenants AS (
    SELECT id
    FROM tenant
    WHERE code IN ('platform', 'demo', 'tchalanet')
),
     tg_games AS (
         SELECT code, id
         FROM game
         WHERE code IN (
             'BORLETTE_3',
             'BORLETTE_4',
             'BORLETTE_5',
             'MARIAGE',
             'LOTTO_3',
             'LOTTO_4',
             'LOTTO_5',
             'LOTTO_5_40',
             'LOTTO_6_50'
         )
     )
INSERT INTO tenant_game (tenant_id, game_id, enabled, display_name, min_stake, max_stake)
SELECT
    t.id        AS tenant_id,
    g.id        AS game_id,
    true        AS enabled,
    CASE g.code
        WHEN 'BORLETTE_3'  THEN 'Borlette 3'
        WHEN 'BORLETTE_4'  THEN 'Borlette 4'
        WHEN 'BORLETTE_5'  THEN 'Borlette 5'
        WHEN 'MARIAGE'     THEN 'Mariage 2 chiffres'
        WHEN 'LOTTO_3'     THEN 'Lotto 3 chiffres'
        WHEN 'LOTTO_4'     THEN 'Lotto 4 chiffres'
        WHEN 'LOTTO_5'     THEN 'Lotto 5 chiffres'
        WHEN 'LOTTO_5_40'  THEN 'Lotto 5/40'
        WHEN 'LOTTO_6_50'  THEN 'Lotto 6/50'
        ELSE g.code
    END        AS display_name,
    1.00       AS min_stake,
    CASE g.code
        WHEN 'BORLETTE_3'  THEN 100.00
        WHEN 'BORLETTE_4'  THEN 100.00
        WHEN 'BORLETTE_5'  THEN 200.00
        WHEN 'MARIAGE'     THEN 100.00
        WHEN 'LOTTO_3'     THEN 200.00
        WHEN 'LOTTO_4'     THEN 200.00
        WHEN 'LOTTO_5'     THEN 200.00
        WHEN 'LOTTO_5_40'  THEN 500.00
        WHEN 'LOTTO_6_50'  THEN 500.00
        ELSE 200.00
    END        AS max_stake
FROM tg_tenants t
         CROSS JOIN tg_games g
ON CONFLICT (tenant_id, game_id) DO NOTHING;


-- ===================
-- 4. PLAN & SUBSCRIPTION (inchangé)
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
-- 6. DRAW_CHANNEL pour les tenants demo + tchalanet
-- ==========================

WITH tenants AS (
    SELECT id AS tenant_id
    FROM tenant
    WHERE code IN ('demo', 'tchalanet')
),
     cfg AS (
         SELECT *
         FROM (VALUES
                      -- US NEW YORK: Numbers 3 & 4
                      ('US_NY_NUM3_MID',  'NY Numbers 3 Midday',   'BORLETTE_3', 'America/New_York',       '14:30:00'::time, 180, 'MON-SAT', true, 10),
                      ('US_NY_NUM3_EVE',  'NY Numbers 3 Evening',  'BORLETTE_3', 'America/New_York',       '22:30:00'::time, 180, 'MON-SAT', true, 20),
                      ('US_NY_NUM4_MID',  'NY Numbers 4 Midday',   'BORLETTE_4', 'America/New_York',       '14:30:00'::time, 180, 'MON-SAT', true, 30),
                      ('US_NY_NUM4_EVE',  'NY Numbers 4 Evening',  'BORLETTE_4', 'America/New_York',       '22:30:00'::time, 180, 'MON-SAT', true, 40),

                      -- US FLORIDA: Pick 3 & Pick 4
                      ('US_FL_PICK3_MID', 'Florida Pick 3 Midday', 'BORLETTE_3', 'America/New_York',       '13:30:00'::time, 180, 'MON-SAT', true, 50),
                      ('US_FL_PICK3_EVE', 'Florida Pick 3 Evening','BORLETTE_3', 'America/New_York',       '21:00:00'::time, 180, 'MON-SAT', true, 60),
                      ('US_FL_PICK4_MID', 'Florida Pick 4 Midday', 'BORLETTE_4', 'America/New_York',       '13:30:00'::time, 180, 'MON-SAT', true, 70),
                      ('US_FL_PICK4_EVE', 'Florida Pick 4 Evening','BORLETTE_4', 'America/New_York',       '21:00:00'::time, 180, 'MON-SAT', true, 80),

                      -- HAITI: Lotto 3/4/5 + Lotto 5/40
                      ('HT_LOTTO_3',      'Lotto 3 chiffres',      'LOTTO_3',    'America/Port-au-Prince', '20:00:00'::time, 300, 'MON-SAT', true, 90),
                      ('HT_LOTTO_4',      'Lotto 4 chiffres',      'LOTTO_4',    'America/Port-au-Prince', '20:00:00'::time, 300, 'MON-SAT', true, 100),
                      ('HT_LOTTO_5',      'Lotto 5 chiffres',      'LOTTO_5',    'America/Port-au-Prince', '20:00:00'::time, 300, 'MON-SAT', true, 110),
                      ('HT_LOTTO_5_40',   'Lotto national 5/40',   'LOTTO_5_40', 'America/Port-au-Prince', '20:00:00'::time, 300, 'WED-SAT-SUN', true, 120)
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
    te.tenant_id,
    c.code,
    c.name,
    g.id             AS game_id,
    c.tz,
    c.draw_time,
    c.cutoff_sec,
    c.days_of_week,
    c.active,
    c.sort_order
FROM tenants te
         JOIN cfg   c ON true
         JOIN game  g ON g.code = c.game_code
ON CONFLICT (tenant_id, code) DO NOTHING;

-- ===================
-- 7. THEME PRESETS GLOBAUX + liaison tenants
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
-- 8. Lier les tenants au thème "tchalanet"
-- ===================
UPDATE tenant
SET active_theme_id = '55500000-0000-0000-0000-000000000001'
WHERE id IN (
             '00000000-0000-0000-0000-000000000001', -- platform
             '00000000-0000-0000-0000-000000000002'  -- demo
    );

