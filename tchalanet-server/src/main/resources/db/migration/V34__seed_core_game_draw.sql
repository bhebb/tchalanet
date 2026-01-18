-- ============================================
-- Seed data for Core Game/Draw domain
-- Extracted from V7__core_game_draw.sql to separate structure and data migrations
-- ============================================

-- ============================================
-- SEED BASE (A..E)
-- ============================================

-- -----------------------------
-- A) Seed games (global catalogue)
-- -----------------------------
WITH g_src AS (
    SELECT *
    FROM (VALUES
              ('HT_BOLET',   'Bolet',    'HAITI', 'SINGLE',         2, 4, 'Jeu haïtien basé sur lots (lot1..lot4).', true,  10),
              ('HT_NUMERO',  'Numéros',  'HAITI', 'EXACT',          2, 4, 'Numéros (exact) dérivés des résultats externes.', true, 20),
              ('HT_MARYAJ',  'Mariage',  'HAITI', 'PAIR_UNORDERED', 2, 2, 'Mariage (paire non ordonnée).', true, 30),
              ('HT_LOTO3',   'Loto 3',   'HAITI', 'EXACT',          3, 3, 'Loto 3 (3 chiffres).', true, 40),
              ('HT_LOTO4',   'Loto 4',   'HAITI', 'EXACT',          4, 4, 'Loto 4 (4 chiffres).', true, 50),
              ('HT_LOTO5',   'Loto 5',   'HAITI', 'EXACT',          5, 5, 'Loto 5 (5 chiffres).', true, 60)
         ) AS v(code, name, category, combination, min_digits, max_digits, description, active, sort_order)
)
INSERT INTO game (id, code, name, category, combination, min_digits, max_digits, description, active, sort_order)
SELECT gen_random_uuid(), code, name, category, combination, min_digits, max_digits, description, active, sort_order
FROM g_src
    ON CONFLICT (code) DO UPDATE
                              SET name = EXCLUDED.name,
                              category = EXCLUDED.category,
                              combination = EXCLUDED.combination,
                              min_digits = EXCLUDED.min_digits,
                              max_digits = EXCLUDED.max_digits,
                              description = EXCLUDED.description,
                              active = EXCLUDED.active,
                              sort_order = EXCLUDED.sort_order;

-- -----------------------------
-- B) Seed tenant_game (default tenant)
-- -----------------------------
WITH t AS (
    SELECT '00000000-0000-0000-0000-000000000003'::uuid AS tenant_id
),
     g AS (
         SELECT id AS game_id, code, name
         FROM game
         WHERE code IN ('HT_BOLET','HT_NUMERO','HT_MARYAJ','HT_LOTO3','HT_LOTO4','HT_LOTO5')
           AND deleted_at IS NULL
     ),
     tg_src AS (
         SELECT
             gen_random_uuid() AS id,
             t.tenant_id,
             g.game_id,
             true AS enabled,
             g.name AS display_name,
             CASE g.code
                 WHEN 'HT_BOLET'  THEN jsonb_build_object('odds', jsonb_build_object('lot2',50,'lot3',20,'lot4',10))
                 WHEN 'HT_MARYAJ' THEN jsonb_build_object('multiplier', 1000)
                 WHEN 'HT_LOTO3'  THEN jsonb_build_object('multiplier', 500)
                 WHEN 'HT_LOTO4'  THEN jsonb_build_object('multiplier', 5000)
                 WHEN 'HT_LOTO5'  THEN jsonb_build_object('multiplier', 25000)
                 ELSE '{}'::jsonb
                 END AS flags,
             NULL::numeric(12,2) AS min_stake,
             NULL::numeric(12,2) AS max_stake
         FROM t CROSS JOIN g
     )
INSERT INTO tenant_game (id, tenant_id, game_id, enabled, display_name, flags, min_stake, max_stake)
SELECT id, tenant_id, game_id, enabled, display_name, flags, min_stake, max_stake
FROM tg_src
    ON CONFLICT (tenant_id, game_id) DO UPDATE
                                            SET enabled = EXCLUDED.enabled,
                                            display_name = EXCLUDED.display_name,
                                            flags = EXCLUDED.flags,
                                            min_stake = EXCLUDED.min_stake,
                                            max_stake = EXCLUDED.max_stake;

-- -----------------------------
-- C) Seed result_slot (global)
--     (projection_cfg = default Haiti rules)
-- -----------------------------
-- Seeds MVP result_slot (NY/FL/GA/TN/TX) with source_cfg + projection_cfg
-- Requires: pgcrypto extension for gen_random_uuid()

CREATE EXTENSION IF NOT EXISTS pgcrypto;

WITH src AS (
    SELECT *
    FROM (VALUES
              -- slotKey,     provider, timezone,            draw_time, days_of_week, active, sort_order, source_cfg, projection_cfg

              -- =========================
              -- NY (Numbers / Win4)
              -- =========================
              ('NY_MID','NY','America/New_York','14:30','MON-SUN', true, 10,
               '{
                 "pick3":{"game_code":"US_NY_NUM3_MID","external_key":"NUMBERS"},
                 "pick4":{"game_code":"US_NY_NUM4_MID","external_key":"WIN4"}
               }'::jsonb,
               '{
                 "version":1,"rule_set":"DEFAULT",
                 "rules":{"lot1":"PICK3_FULL_3","lot2":"PICK4_FIRST2","lot3":"PICK4_LAST2","lot4":"PICK3_FIRST2"}
               }'::jsonb
              ),

              ('NY_EVE','NY','America/New_York','22:30','MON-SUN', true, 11,
               '{
                 "pick3":{"game_code":"US_NY_NUM3_EVE","external_key":"NUMBERS"},
                 "pick4":{"game_code":"US_NY_NUM4_EVE","external_key":"WIN4"}
               }'::jsonb,
               '{
                 "version":1,"rule_set":"DEFAULT",
                 "rules":{"lot1":"PICK3_FULL_3","lot2":"PICK4_FIRST2","lot3":"PICK4_LAST2","lot4":"PICK3_FIRST2"}
               }'::jsonb
              ),

              -- =========================
              -- FL (Pick 3 / Pick 4)
              -- =========================
              ('FL_MID','FL','America/New_York','13:30','MON-SUN', true, 20,
               '{
                 "pick3":{"game_code":"US_FL_PICK3_MID","external_key":"PICK3"},
                 "pick4":{"game_code":"US_FL_PICK4_MID","external_key":"PICK4"}
               }'::jsonb,
               '{
                 "version":1,"rule_set":"DEFAULT",
                 "rules":{"lot1":"PICK3_FULL_3","lot2":"PICK4_FIRST2","lot3":"PICK4_LAST2","lot4":"PICK3_FIRST2"}
               }'::jsonb
              ),

              ('FL_EVE','FL','America/New_York','22:45','MON-SUN', true, 21,
               '{
                 "pick3":{"game_code":"US_FL_PICK3_EVE","external_key":"PICK3"},
                 "pick4":{"game_code":"US_FL_PICK4_EVE","external_key":"PICK4"}
               }'::jsonb,
               '{
                 "version":1,"rule_set":"DEFAULT",
                 "rules":{"lot1":"PICK3_FULL_3","lot2":"PICK4_FIRST2","lot3":"PICK4_LAST2","lot4":"PICK3_FIRST2"}
               }'::jsonb
              ),

              -- =========================
              -- GA (Cash 3 / Cash 4)
              -- =========================
              ('GA_MID','GA','America/New_York','12:29','MON-SUN', true, 30,
               '{
                 "pick3":{"game_code":"US_GA_CASH3_1229","external_key":"CASH3"},
                 "pick4":{"game_code":"US_GA_CASH4_1229","external_key":"CASH4"}
               }'::jsonb,
               '{
                 "version":1,"rule_set":"DEFAULT",
                 "rules":{"lot1":"PICK3_FULL_3","lot2":"PICK4_FIRST2","lot3":"PICK4_LAST2","lot4":"PICK3_FIRST2"}
               }'::jsonb
              ),

              ('GA_EVE','GA','America/New_York','18:59','MON-SUN', true, 31,
               '{
                 "pick3":{"game_code":"US_GA_CASH3_1859","external_key":"CASH3"},
                 "pick4":{"game_code":"US_GA_CASH4_1859","external_key":"CASH4"}
               }'::jsonb,
               '{
                 "version":1,"rule_set":"DEFAULT",
                 "rules":{"lot1":"PICK3_FULL_3","lot2":"PICK4_FIRST2","lot3":"PICK4_LAST2","lot4":"PICK3_FIRST2"}
               }'::jsonb
              ),

              ('GA_LATE','GA','America/New_York','23:34','MON-SUN', true, 32,
               '{
                 "pick3":{"game_code":"US_GA_CASH3_2334","external_key":"CASH3"},
                 "pick4":{"game_code":"US_GA_CASH4_2334","external_key":"CASH4"}
               }'::jsonb,
               '{
                 "version":1,"rule_set":"DEFAULT",
                 "rules":{"lot1":"PICK3_FULL_3","lot2":"PICK4_FIRST2","lot3":"PICK4_LAST2","lot4":"PICK3_FIRST2"}
               }'::jsonb
              ),

              -- =========================
              -- TN (MVP: keep inactive by default)
              -- =========================
              ('TN_MID','TN','America/Chicago','12:55','MON-SAT', false, 40,
               '{
                 "pick3":{"game_code":"US_TN_CASH3_1255","external_key":"CASH3"},
                 "pick4":{"game_code":"US_TN_CASH4_1255","external_key":"CASH4"}
               }'::jsonb,
               '{
                 "version":1,"rule_set":"DEFAULT",
                 "rules":{"lot1":"PICK3_FULL_3","lot2":"PICK4_FIRST2","lot3":"PICK4_LAST2","lot4":"PICK3_FIRST2"}
               }'::jsonb
              ),

              -- =========================
              -- TX (Pick3 / Daily4)
              -- =========================
              ('TX_1000','TX','America/Chicago','10:00','MON-SAT', true, 50,
               '{
                 "pick3":{"game_code":"US_TX_PICK3_1000","external_key":"PICK3"},
                 "pick4":{"game_code":"US_TX_DAILY4_1000","external_key":"DAILY4"}
               }'::jsonb,
               '{
                 "version":1,"rule_set":"DEFAULT",
                 "rules":{"lot1":"PICK3_FULL_3","lot2":"PICK4_FIRST2","lot3":"PICK4_LAST2","lot4":"PICK3_FIRST2"}
               }'::jsonb
              ),

              ('TX_1227','TX','America/Chicago','12:27','MON-SAT', true, 51,
               '{
                 "pick3":{"game_code":"US_TX_PICK3_1227","external_key":"PICK3"},
                 "pick4":{"game_code":"US_TX_DAILY4_1227","external_key":"DAILY4"}
               }'::jsonb,
               '{
                 "version":1,"rule_set":"DEFAULT",
                 "rules":{"lot1":"PICK3_FULL_3","lot2":"PICK4_FIRST2","lot3":"PICK4_LAST2","lot4":"PICK3_FIRST2"}
               }'::jsonb
              ),

              ('TX_1800','TX','America/Chicago','18:00','MON-SAT', true, 52,
               '{
                 "pick3":{"game_code":"US_TX_PICK3_1800","external_key":"PICK3"},
                 "pick4":{"game_code":"US_TX_DAILY4_1800","external_key":"DAILY4"}
               }'::jsonb,
               '{
                 "version":1,"rule_set":"DEFAULT",
                 "rules":{"lot1":"PICK3_FULL_3","lot2":"PICK4_FIRST2","lot3":"PICK4_LAST2","lot4":"PICK3_FIRST2"}
               }'::jsonb
              ),

              ('TX_2212','TX','America/Chicago','22:12','MON-SAT', true, 53,
               '{
                 "pick3":{"game_code":"US_TX_PICK3_2212","external_key":"PICK3"},
                 "pick4":{"game_code":"US_TX_DAILY4_2212","external_key":"DAILY4"}
               }'::jsonb,
               '{
                 "version":1,"rule_set":"DEFAULT",
                 "rules":{"lot1":"PICK3_FULL_3","lot2":"PICK4_FIRST2","lot3":"PICK4_LAST2","lot4":"PICK3_FIRST2"}
               }'::jsonb
              )

         ) AS v(key, provider, timezone, draw_time, days_of_week, active, sort_order, source_cfg, projection_cfg)
)
INSERT INTO result_slot (
  id, key, provider, timezone, draw_time, days_of_week,
  active, sort_order, source_cfg, projection_cfg,
  created_at, updated_at, version
)
SELECT
    gen_random_uuid(),
    upper(key),
    upper(provider),
    timezone,
    draw_time::time,
    days_of_week,
    active,
    sort_order,
    source_cfg,
    projection_cfg,
    now(), now(), 0
FROM src
    ON CONFLICT (key) DO UPDATE
                             SET
                                 provider       = EXCLUDED.provider,
                             timezone       = EXCLUDED.timezone,
                             draw_time      = EXCLUDED.draw_time,
                             days_of_week   = EXCLUDED.days_of_week,
                             active         = EXCLUDED.active,
                             sort_order     = EXCLUDED.sort_order,
                             source_cfg     = EXCLUDED.source_cfg,
                             projection_cfg = EXCLUDED.projection_cfg,
                             updated_at     = now(),
                             version        = result_slot.version + 1
                         WHERE result_slot.deleted_at IS NULL;

-- -----------------------------
-- D) Seed draw_channel (global)
-- -----------------------------
-- Seeds draw_channel for default tenant, each channel points to a global result_slot
-- Tenant default: 00000000-0000-0000-0000-000000000003

CREATE EXTENSION IF NOT EXISTS pgcrypto;

WITH t AS (
    SELECT '00000000-0000-0000-0000-000000000003'::uuid AS tenant_id
),
     rs AS (
         SELECT id, key, provider, timezone, draw_time
FROM result_slot
WHERE deleted_at IS NULL
    ),
    src AS (
SELECT *
FROM (VALUES
    -- channel_code,  name,                         result_slot_key, cutoff_sec, days_of_week, active, sort_order
    ('HT_NY_MID', 'Haïti • New York • Midday',     'NY_MID', 300, 'MON-SUN', true, 10),
    ('HT_NY_EVE', 'Haïti • New York • Evening',    'NY_EVE', 300, 'MON-SUN', true, 11),

    ('HT_FL_MID', 'Haïti • Florida • Midday',      'FL_MID', 400, 'MON-SUN', true, 20),
    ('HT_FL_EVE', 'Haïti • Florida • Evening',     'FL_EVE', 500, 'MON-SUN', true, 21),

    ('HT_GA_MID', 'Haïti • Georgia • Midday',      'GA_MID', 300, 'MON-SUN', true, 30),
    ('HT_GA_EVE', 'Haïti • Georgia • Evening',     'GA_EVE', 300, 'MON-SUN', true, 31),
    ('HT_GA_LATE','Haïti • Georgia • Late',        'GA_LATE',300, 'MON-SUN', true, 32),

    -- TN MVP off by default (slot inactive too)
    ('HT_TN_MID', 'Haïti • Tennessee • Midday',    'TN_MID', 300, 'MON-SAT', false, 40),

    ('HT_TX_1000','Haïti • Texas • 10:00',         'TX_1000',300, 'MON-SAT', true, 50),
    ('HT_TX_1227','Haïti • Texas • 12:27',         'TX_1227',300, 'MON-SAT', true, 51),
    ('HT_TX_1800','Haïti • Texas • 18:00',         'TX_1800',300, 'MON-SAT', true, 52),
    ('HT_TX_2212','Haïti • Texas • 22:12',         'TX_2212',300, 'MON-SAT', true, 53)

    ) AS v(code, name, slot_key, cutoff_sec, days_of_week, active, sort_order)
    ),
    rows AS (
SELECT
    gen_random_uuid() AS id,
    (SELECT tenant_id FROM t) AS tenant_id,
    s.code,
    s.name,
    r.timezone,
    r.draw_time,
    s.cutoff_sec,
    s.days_of_week,
    s.active,
    s.sort_order,
    r.provider AS external_provider,
    r.id AS result_slot_id,
    '{}'::jsonb AS flags
FROM src s
    JOIN rs r ON r.key = s.slot_key
    )
INSERT INTO draw_channel (
    id, tenant_id,
    code, name,
    timezone, draw_time, cutoff_sec, days_of_week,
    active, sort_order,
    external_provider,
    result_slot_id,
    flags,
    notes,
    created_at, updated_at, version
)
SELECT
    id, tenant_id,
    code, name,
    timezone, draw_time, cutoff_sec, days_of_week,
    active, sort_order,
    external_provider,
    result_slot_id,
    flags,
    NULL::text,
    now(), now(), 0
FROM rows
    ON CONFLICT (tenant_id, code) DO UPDATE
                                         SET
                                             name            = EXCLUDED.name,
                                         timezone        = EXCLUDED.timezone,
                                         draw_time       = EXCLUDED.draw_time,
                                         cutoff_sec      = EXCLUDED.cutoff_sec,
                                         days_of_week    = EXCLUDED.days_of_week,
                                         active          = EXCLUDED.active,
                                         sort_order      = EXCLUDED.sort_order,
                                         external_provider = EXCLUDED.external_provider,
                                         result_slot_id  = EXCLUDED.result_slot_id,
                                         flags           = EXCLUDED.flags,
                                         updated_at      = now(),
                                         version         = draw_channel.version + 1
                                     WHERE draw_channel.deleted_at IS NULL;


-- -----------------------------
-- E) Seed draw_channel_game (global)
-- -----------------------------
WITH cg_src AS (
    SELECT *-- Links Haiti games to all seeded channels for the default tenant

WITH t AS (
    SELECT '00000000-0000-0000-0000-000000000003'::uuid AS tenant_id
    ),
    channels AS (
    SELECT id AS draw_channel_id
    FROM draw_channel
    WHERE tenant_id = (SELECT tenant_id FROM t)
    AND deleted_at IS NULL
    ),
    games AS (
    SELECT id AS game_id
    FROM game
    WHERE code IN ('HT_BOLET','HT_NUMERO','HT_MARYAJ','HT_LOTO3','HT_LOTO4','HT_LOTO5')
    AND deleted_at IS NULL
    ),
    pairs AS (
    SELECT
    gen_random_uuid() AS id,
    (SELECT tenant_id FROM t) AS tenant_id,
    c.draw_channel_id,
    g.game_id,
    true AS enabled,
    '{}'::jsonb AS flags
    FROM channels c CROSS JOIN games g
    )
INSERT INTO draw_channel_game (id, tenant_id, draw_channel_id, game_id, enabled, flags, created_at, updated_at, version)
SELECT id, tenant_id, draw_channel_id, game_id, enabled, flags, now(), now(), 0
FROM pairs
    ON CONFLICT (tenant_id, draw_channel_id, game_id) DO UPDATE
                                                             SET enabled = EXCLUDED.enabled,
                                                             flags = EXCLUDED.flags,
                                                             updated_at = now(),
                                                             version = draw_channel_game.version + 1
                                                         WHERE draw_channel_game.deleted_at IS NULL;
