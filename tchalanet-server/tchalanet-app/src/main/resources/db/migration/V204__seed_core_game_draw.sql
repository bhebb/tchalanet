-- ============================================
-- Seed data for Core Game/Draw domain
-- ============================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Flyway RLS context for default tenant seed
SELECT set_config('app.current_tenant', '00000000-0000-0000-0000-000000000003', true);
SELECT set_config('app.deleted_visibility', 'active', true);
SELECT set_config('app.api_scope', 'tenant', true);
SELECT set_config('app.is_super_admin', 'false', true);

-- -----------------------------
-- A) Seed games
-- -----------------------------
WITH g_src AS (
    SELECT *
    FROM (VALUES
              ('HT_BOLET',  'Bolet',   'HAITI', 'SINGLE',         2, 4, 'Jeu haïtien basé sur lots (lot1..lot4).', true, 10),
              ('HT_NUMERO', 'Numéros', 'HAITI', 'EXACT',          2, 4, 'Numéros exacts dérivés des résultats externes.', true, 20),
              ('HT_MARYAJ', 'Mariage', 'HAITI', 'PAIR_UNORDERED', 2, 2, 'Mariage, paire non ordonnée.', true, 30),
              ('HT_MARYAJ_GRATUIT', 'Maryaj gratuit', 'HAITI', 'PAIR_UNORDERED', 2, 2, 'Variante promotionnelle du Maryaj.', true, 35),
              ('HT_LOTO3',  'Loto 3',  'HAITI', 'EXACT',          3, 3, 'Loto 3, 3 chiffres.', true, 40),
              ('HT_LOTO4',  'Loto 4',  'HAITI', 'EXACT',          4, 4, 'Loto 4, 4 chiffres.', true, 50),
              ('HT_LOTO5',  'Loto 5',  'HAITI', 'EXACT',          5, 5, 'Loto 5, 5 chiffres.', true, 60)
         ) AS v(code, name, category, combination, min_digits, max_digits, description, active, sort_order)
)
INSERT INTO game (
  id, code, name, category, combination,
  min_digits, max_digits, description, active, sort_order,
  created_at, updated_at, version
)
SELECT
    gen_random_uuid(), code, name, category, combination,
    min_digits, max_digits, description, active, sort_order,
    now(), now(), 0
FROM g_src
    ON CONFLICT (code) DO UPDATE
                              SET name        = EXCLUDED.name,
                              category    = EXCLUDED.category,
                              combination = EXCLUDED.combination,
                              min_digits  = EXCLUDED.min_digits,
                              max_digits  = EXCLUDED.max_digits,
                              description = EXCLUDED.description,
                              active      = EXCLUDED.active,
                              sort_order  = EXCLUDED.sort_order,
                              updated_at  = now(),
                              version     = game.version + 1
                          WHERE game.deleted_at IS NULL;

-- -----------------------------
-- B) Seed tenant_game
-- -----------------------------
WITH t AS (
    SELECT '00000000-0000-0000-0000-000000000003'::uuid AS tenant_id
),
     g AS (
         SELECT id AS game_id, code, name
          FROM game
          WHERE code IN ('HT_BOLET','HT_NUMERO','HT_MARYAJ','HT_MARYAJ_GRATUIT','HT_LOTO3','HT_LOTO4','HT_LOTO5')
           AND deleted_at IS NULL
     ),
     tg_src AS (
         SELECT
             gen_random_uuid() AS id,
             t.tenant_id,
             g.game_id,
             g.code AS game_code,
             true AS enabled,
             g.name AS display_name,
             NULL::numeric(12,2) AS min_stake,
             NULL::numeric(12,2) AS max_stake
         FROM t CROSS JOIN g
     )
INSERT INTO tenant_game (
  id, tenant_id, game_id, game_code, enabled, display_name,
  min_stake, max_stake,
  created_at, updated_at, version
)
SELECT
    id, tenant_id, game_id, game_code, enabled, display_name,
    min_stake, max_stake,
    now(), now(), 0
FROM tg_src
    ON CONFLICT (tenant_id, game_id) DO UPDATE
                                            SET enabled      = EXCLUDED.enabled,
                                            display_name = EXCLUDED.display_name,
                                            min_stake    = EXCLUDED.min_stake,
                                            max_stake    = EXCLUDED.max_stake,
                                            updated_at   = now(),
                                            version      = tenant_game.version + 1
                                        WHERE tenant_game.deleted_at IS NULL;

-- -----------------------------
-- C) Seed result_slot
-- -----------------------------
WITH src AS (
    SELECT *
    FROM (VALUES
                 ('NY_MID','NY','America/New_York','14:30','MON-SUN', true, 10,
                  '{"provider_slot_code":"MIDDAY","pick3":{"game_code":"NUMBERS","active":true},"pick4":{"game_code":"WIN4","active":true}}'::jsonb,
                  '{"version":1,"rule_set":"DEFAULT","rules":{"lot1":"PICK3_FULL_3","lot2":"PICK4_FIRST2","lot3":"PICK4_LAST2","lot4":"PICK3_FIRST2"}}'::jsonb),

                 ('NY_EVE','NY','America/New_York','22:30','MON-SUN', true, 11,
                  '{"provider_slot_code":"EVENING","pick3":{"game_code":"NUMBERS","active":true},"pick4":{"game_code":"WIN4","active":true}}'::jsonb,
                  '{"version":1,"rule_set":"DEFAULT","rules":{"lot1":"PICK3_FULL_3","lot2":"PICK4_FIRST2","lot3":"PICK4_LAST2","lot4":"PICK3_FIRST2"}}'::jsonb),

                 ('FL_MID','FL','America/New_York','13:30','MON-SUN', true, 20,
                  '{"provider_slot_code":"MIDDAY","pick3":{"game_code":"PICK3","active":true},"pick4":{"game_code":"PICK4","active":true}}'::jsonb,
                  '{"version":1,"rule_set":"DEFAULT","rules":{"lot1":"PICK3_FULL_3","lot2":"PICK4_FIRST2","lot3":"PICK4_LAST2","lot4":"PICK3_FIRST2"}}'::jsonb),

                 ('FL_EVE','FL','America/New_York','22:45','MON-SUN', true, 21,
                  '{"provider_slot_code":"EVENING","pick3":{"game_code":"PICK3","active":true},"pick4":{"game_code":"PICK4","active":true}}'::jsonb,
                  '{"version":1,"rule_set":"DEFAULT","rules":{"lot1":"PICK3_FULL_3","lot2":"PICK4_FIRST2","lot3":"PICK4_LAST2","lot4":"PICK3_FIRST2"}}'::jsonb),

                 ('GA_MID','GA','America/New_York','12:29','MON-SUN', true, 30,
                  '{"provider_slot_code":"MIDDAY","pick3":{"game_code":"CASH3","active":true},"pick4":{"game_code":"CASH4","active":true}}'::jsonb,
                  '{"version":1,"rule_set":"DEFAULT","rules":{"lot1":"PICK3_FULL_3","lot2":"PICK4_FIRST2","lot3":"PICK4_LAST2","lot4":"PICK3_FIRST2"}}'::jsonb),

                 ('GA_EVE','GA','America/New_York','18:59','MON-SUN', true, 31,
                  '{"provider_slot_code":"EVENING","pick3":{"game_code":"CASH3","active":true},"pick4":{"game_code":"CASH4","active":true}}'::jsonb,
                  '{"version":1,"rule_set":"DEFAULT","rules":{"lot1":"PICK3_FULL_3","lot2":"PICK4_FIRST2","lot3":"PICK4_LAST2","lot4":"PICK3_FIRST2"}}'::jsonb),

                 ('GA_LATE','GA','America/New_York','23:34','MON-SUN', true, 32,
                  '{"provider_slot_code":"NIGHT","pick3":{"game_code":"CASH3","active":true},"pick4":{"game_code":"CASH4","active":true}}'::jsonb,
                  '{"version":1,"rule_set":"DEFAULT","rules":{"lot1":"PICK3_FULL_3","lot2":"PICK4_FIRST2","lot3":"PICK4_LAST2","lot4":"PICK3_FIRST2"}}'::jsonb),

                 ('TX_1000','TX','America/Chicago','10:00','MON-SAT', true, 50,
                  '{"provider_slot_code":"MORNING","pick3":{"game_code":"PICK3","active":true},"pick4":{"game_code":"DAILY4","active":true}}'::jsonb,
                  '{"version":1,"rule_set":"DEFAULT","rules":{"lot1":"PICK3_FULL_3","lot2":"PICK4_FIRST2","lot3":"PICK4_LAST2","lot4":"PICK3_FIRST2"}}'::jsonb),

                 ('TX_1227','TX','America/Chicago','12:27','MON-SAT', true, 51,
                  '{"provider_slot_code":"DAY","pick3":{"game_code":"PICK3","active":true},"pick4":{"game_code":"DAILY4","active":true}}'::jsonb,
                  '{"version":1,"rule_set":"DEFAULT","rules":{"lot1":"PICK3_FULL_3","lot2":"PICK4_FIRST2","lot3":"PICK4_LAST2","lot4":"PICK3_FIRST2"}}'::jsonb),

                 ('TX_1800','TX','America/Chicago','18:00','MON-SAT', true, 52,
                  '{"provider_slot_code":"EVENING","pick3":{"game_code":"PICK3","active":true},"pick4":{"game_code":"DAILY4","active":true}}'::jsonb,
                  '{"version":1,"rule_set":"DEFAULT","rules":{"lot1":"PICK3_FULL_3","lot2":"PICK4_FIRST2","lot3":"PICK4_LAST2","lot4":"PICK3_FIRST2"}}'::jsonb),

                 ('TX_2212','TX','America/Chicago','22:12','MON-SAT', true, 53,
                  '{"provider_slot_code":"NIGHT","pick3":{"game_code":"PICK3","active":true},"pick4":{"game_code":"DAILY4","active":true}}'::jsonb,
                  '{"version":1,"rule_set":"DEFAULT","rules":{"lot1":"PICK3_FULL_3","lot2":"PICK4_FIRST2","lot3":"PICK4_LAST2","lot4":"PICK3_FIRST2"}}'::jsonb),

                 ('PA_MID','PA','America/New_York','13:35','MON-SUN', true, 60,
                  '{"provider_slot_code":"MIDDAY","pick3":{"game_code":"PICK3","active":true},"pick4":{"game_code":"PICK4","active":true}}'::jsonb,
                  '{"version":1,"rule_set":"DEFAULT","rules":{"lot1":"PICK3_FULL_3","lot2":"PICK4_FIRST2","lot3":"PICK4_LAST2","lot4":"PICK3_FIRST2"}}'::jsonb),

                 ('PA_EVE','PA','America/New_York','19:00','MON-SUN', true, 61,
                  '{"provider_slot_code":"EVENING","pick3":{"game_code":"PICK3","active":true},"pick4":{"game_code":"PICK4","active":true}}'::jsonb,
                  '{"version":1,"rule_set":"DEFAULT","rules":{"lot1":"PICK3_FULL_3","lot2":"PICK4_FIRST2","lot3":"PICK4_LAST2","lot4":"PICK3_FIRST2"}}'::jsonb),

                 ('NJ_MID','NJ','America/New_York','12:59','MON-SUN', true, 70,
                  '{"provider_slot_code":"MIDDAY","pick3":{"game_code":"PICK3","active":true},"pick4":{"game_code":"PICK4","active":true}}'::jsonb,
                  '{"version":1,"rule_set":"DEFAULT","rules":{"lot1":"PICK3_FULL_3","lot2":"PICK4_FIRST2","lot3":"PICK4_LAST2","lot4":"PICK3_FIRST2"}}'::jsonb),

                 ('NJ_EVE','NJ','America/New_York','22:57','MON-SUN', true, 71,
                  '{"provider_slot_code":"EVENING","pick3":{"game_code":"PICK3","active":true},"pick4":{"game_code":"PICK4","active":true}}'::jsonb,
                  '{"version":1,"rule_set":"DEFAULT","rules":{"lot1":"PICK3_FULL_3","lot2":"PICK4_FIRST2","lot3":"PICK4_LAST2","lot4":"PICK3_FIRST2"}}'::jsonb),

                 ('CA_MID','CA','America/Los_Angeles','13:00','MON-SUN', true, 80,
                  '{"provider_slot_code":"MIDDAY","pick3":{"game_code":"DAILY3","active":true},"pick4":{"game_code":"DAILY4","active":true}}'::jsonb,
                  '{"version":1,"rule_set":"DEFAULT","rules":{"lot1":"PICK3_FULL_3","lot2":"PICK4_FIRST2","lot3":"PICK4_LAST2","lot4":"PICK3_FIRST2"}}'::jsonb),

                 ('CA_EVE','CA','America/Los_Angeles','18:30','MON-SUN', true, 81,
                  '{"provider_slot_code":"EVENING","pick3":{"game_code":"DAILY3","active":true},"pick4":{"game_code":"DAILY4","active":true}}'::jsonb,
                  '{"version":1,"rule_set":"DEFAULT","rules":{"lot1":"PICK3_FULL_3","lot2":"PICK4_FIRST2","lot3":"PICK4_LAST2","lot4":"PICK3_FIRST2"}}'::jsonb),

                 ('OH_MID','OH','America/New_York','12:29','MON-SUN', true, 90,
                  '{"provider_slot_code":"MIDDAY","pick3":{"game_code":"PICK3","active":true},"pick4":{"game_code":"PICK4","active":true}}'::jsonb,
                  '{"version":1,"rule_set":"DEFAULT","rules":{"lot1":"PICK3_FULL_3","lot2":"PICK4_FIRST2","lot3":"PICK4_LAST2","lot4":"PICK3_FIRST2"}}'::jsonb),

                 ('OH_EVE','OH','America/New_York','19:29','MON-SUN', true, 91,
                  '{"provider_slot_code":"EVENING","pick3":{"game_code":"PICK3","active":true},"pick4":{"game_code":"PICK4","active":true}}'::jsonb,
                  '{"version":1,"rule_set":"DEFAULT","rules":{"lot1":"PICK3_FULL_3","lot2":"PICK4_FIRST2","lot3":"PICK4_LAST2","lot4":"PICK3_FIRST2"}}'::jsonb),

                 ('MI_MID','MI','America/Detroit','12:59','MON-SUN', true, 100,
                  '{"provider_slot_code":"MIDDAY","pick3":{"game_code":"PICK3","active":true},"pick4":{"game_code":"PICK4","active":true}}'::jsonb,
                  '{"version":1,"rule_set":"DEFAULT","rules":{"lot1":"PICK3_FULL_3","lot2":"PICK4_FIRST2","lot3":"PICK4_LAST2","lot4":"PICK3_FIRST2"}}'::jsonb),

                 ('MI_EVE','MI','America/Detroit','19:29','MON-SUN', true, 101,
                  '{"provider_slot_code":"EVENING","pick3":{"game_code":"PICK3","active":true},"pick4":{"game_code":"PICK4","active":true}}'::jsonb,
                  '{"version":1,"rule_set":"DEFAULT","rules":{"lot1":"PICK3_FULL_3","lot2":"PICK4_FIRST2","lot3":"PICK4_LAST2","lot4":"PICK3_FIRST2"}}'::jsonb),

                 ('TN_MID','TN','America/Chicago','12:29','MON-SAT', false, 40,
                  '{"provider_slot_code":"MIDDAY","pick3":{"game_code":"CASH3","active":true},"pick4":{"game_code":"CASH4","active":true}}'::jsonb,
                  '{"version":1,"rule_set":"DEFAULT","rules":{"lot1":"PICK3_FULL_3","lot2":"PICK4_FIRST2","lot3":"PICK4_LAST2","lot4":"PICK3_FIRST2"}}'::jsonb),

                 ('TN_EVE','TN','America/Chicago','22:00','MON-SAT', false, 41,
                  '{"provider_slot_code":"EVENING","pick3":{"game_code":"CASH3","active":true},"pick4":{"game_code":"CASH4","active":true}}'::jsonb,
                  '{"version":1,"rule_set":"DEFAULT","rules":{"lot1":"PICK3_FULL_3","lot2":"PICK4_FIRST2","lot3":"PICK4_LAST2","lot4":"PICK3_FIRST2"}}'::jsonb),

                 ('IL_MID','IL','America/Chicago','12:40','MON-SUN', false, 110,
                  '{"provider_slot_code":"MIDDAY","pick3":{"game_code":"PICK3","active":true},"pick4":{"game_code":"PICK4","active":true}}'::jsonb,
                  '{"version":1,"rule_set":"DEFAULT","rules":{"lot1":"PICK3_FULL_3","lot2":"PICK4_FIRST2","lot3":"PICK4_LAST2","lot4":"PICK3_FIRST2"}}'::jsonb),

                 ('IL_EVE','IL','America/Chicago','21:22','MON-SUN', false, 111,
                  '{"provider_slot_code":"EVENING","pick3":{"game_code":"PICK3","active":true},"pick4":{"game_code":"PICK4","active":true}}'::jsonb,
                  '{"version":1,"rule_set":"DEFAULT","rules":{"lot1":"PICK3_FULL_3","lot2":"PICK4_FIRST2","lot3":"PICK4_LAST2","lot4":"PICK3_FIRST2"}}'::jsonb),

                 ('MO_MID','MO','America/Chicago','12:45','MON-SUN', false, 120,
                  '{"provider_slot_code":"MIDDAY","pick3":{"game_code":"PICK3","active":true},"pick4":{"game_code":"PICK4","active":true}}'::jsonb,
                  '{"version":1,"rule_set":"DEFAULT","rules":{"lot1":"PICK3_FULL_3","lot2":"PICK4_FIRST2","lot3":"PICK4_LAST2","lot4":"PICK3_FIRST2"}}'::jsonb),

                 ('MO_EVE','MO','America/Chicago','21:00','MON-SUN', false, 121,
                  '{"provider_slot_code":"EVENING","pick3":{"game_code":"PICK3","active":true},"pick4":{"game_code":"PICK4","active":true}}'::jsonb,
                  '{"version":1,"rule_set":"DEFAULT","rules":{"lot1":"PICK3_FULL_3","lot2":"PICK4_FIRST2","lot3":"PICK4_LAST2","lot4":"PICK3_FIRST2"}}'::jsonb),

                 ('MN_EVE','MN','America/Chicago','18:17','MON-SUN', true, 130,
                  '{"provider_slot_code":"EVENING","pick3":{"game_code":"PICK3","active":true}}'::jsonb,
                  '{"version":1,"rule_set":"DEFAULT","rules":{"lot1":"PICK3_FULL_3","lot4":"PICK3_FIRST2"}}'::jsonb)

         ) AS v(slot_key, provider, timezone, draw_time, days_of_week, active, sort_order, source_cfg, projection_cfg)
)
INSERT INTO result_slot (
  id, slot_key, provider, timezone, draw_time, days_of_week,
  active, sort_order, source_cfg, projection_cfg,
  created_at, updated_at, version
)
SELECT
    gen_random_uuid(),
    upper(slot_key),
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
    ON CONFLICT (slot_key) DO UPDATE
                                  SET provider       = EXCLUDED.provider,
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
-- D) Seed draw_channel
-- -----------------------------
WITH t AS (
    SELECT '00000000-0000-0000-0000-000000000003'::uuid AS tenant_id
),
     rs AS (
         SELECT id, slot_key, timezone, draw_time
         FROM result_slot
         WHERE deleted_at IS NULL
     ),
     src AS (
         SELECT *
         FROM (VALUES
                   ('HT_NY_MID', 'Haïti • New York • Midday',  'NY_MID', 300, 'MON-SUN', true, 10),
                   ('HT_NY_EVE', 'Haïti • New York • Evening', 'NY_EVE', 300, 'MON-SUN', true, 11),
                   ('HT_FL_MID', 'Haïti • Florida • Midday',   'FL_MID', 400, 'MON-SUN', true, 20),
                   ('HT_FL_EVE', 'Haïti • Florida • Evening',  'FL_EVE', 500, 'MON-SUN', true, 21),
                   ('HT_GA_MID', 'Haïti • Georgia • Midday',   'GA_MID', 300, 'MON-SUN', true, 30),
                   ('HT_GA_EVE', 'Haïti • Georgia • Evening',  'GA_EVE', 300, 'MON-SUN', true, 31),
                   ('HT_GA_LATE','Haïti • Georgia • Late',     'GA_LATE',300, 'MON-SUN', true, 32),
                   ('HT_TN_MID', 'Haïti • Tennessee • Midday', 'TN_MID', 300, 'MON-SAT', true, 40),
                   ('HT_TX_1000','Haïti • Texas • 10:00',      'TX_1000',300, 'MON-SAT', true, 50),
                   ('HT_TX_1227','Haïti • Texas • 12:27',      'TX_1227',300, 'MON-SAT', true, 51),
                   ('HT_TX_1800','Haïti • Texas • 18:00',      'TX_1800',300, 'MON-SAT', true, 52),
                   ('HT_TX_2212','Haïti • Texas • 22:12',      'TX_2212',300, 'MON-SAT', true,  53),
                   ('HT_TN_EVE', 'Haïti • Tennessee • Evening','TN_EVE', 300, 'MON-SAT', true, 41),
                   ('HT_PA_MID', 'Haïti • Pennsylvania • Midday',  'PA_MID', 300, 'MON-SUN', true,  60),
                   ('HT_PA_EVE', 'Haïti • Pennsylvania • Evening', 'PA_EVE', 300, 'MON-SUN', true,  61),
                   ('HT_NJ_MID', 'Haïti • New Jersey • Midday',    'NJ_MID', 300, 'MON-SUN', true,  70),
                   ('HT_NJ_EVE', 'Haïti • New Jersey • Evening',   'NJ_EVE', 300, 'MON-SUN', true,  71),
                   ('HT_CA_MID', 'Haïti • California • Midday',    'CA_MID', 300, 'MON-SUN', true,  80),
                   ('HT_CA_EVE', 'Haïti • California • Evening',   'CA_EVE', 300, 'MON-SUN', true,  81),
                   ('HT_OH_MID', 'Haïti • Ohio • Midday',          'OH_MID', 300, 'MON-SUN', true,  90),
                   ('HT_OH_EVE', 'Haïti • Ohio • Evening',         'OH_EVE', 300, 'MON-SUN', true,  91),
                   ('HT_MI_MID', 'Haïti • Michigan • Midday',      'MI_MID', 300, 'MON-SUN', true,  100),
                   ('HT_MI_EVE', 'Haïti • Michigan • Evening',     'MI_EVE', 300, 'MON-SUN', true,  101),
                   ('HT_IL_MID', 'Haïti • Illinois • Midday',      'IL_MID', 300, 'MON-SUN', true, 110),
                   ('HT_IL_EVE', 'Haïti • Illinois • Evening',     'IL_EVE', 300, 'MON-SUN', true, 111),
                   ('HT_MO_MID', 'Haïti • Missouri • Midday',      'MO_MID', 300, 'MON-SUN', true, 120),
                   ('HT_MO_EVE', 'Haïti • Missouri • Evening',     'MO_EVE', 300, 'MON-SUN', true, 121),
                   ('HT_MN_EVE', 'Haïti • Minnesota • Evening',    'MN_EVE', 300, 'MON-SUN', true, 130)
              ) AS v(code, name, slot_key, cutoff_sec, days_of_week, active, sort_order)
     ),
     rows AS (
         SELECT
             gen_random_uuid() AS id,
             t.tenant_id,
             s.code,
             s.name,
             rs.timezone,
             rs.draw_time,
             '05:30'::time AS sales_open_time,
             s.cutoff_sec,
             s.days_of_week,
             s.active,
             s.sort_order,
             '{}'::jsonb AS flags,
             rs.id AS result_slot_id
         FROM src s
                  CROSS JOIN t
                  JOIN rs ON rs.slot_key = s.slot_key
     )
INSERT INTO draw_channel (
  id, tenant_id, code, name,
  timezone, draw_time, sales_open_time, cutoff_sec, days_of_week,
  active, sort_order, flags, result_slot_id,
  notes, created_at, updated_at, version
)
SELECT
    id, tenant_id, code, name,
    timezone, draw_time, sales_open_time, cutoff_sec, days_of_week,
    active, sort_order, flags, result_slot_id,
    NULL::text, now(), now(), 0
FROM rows
    ON CONFLICT (tenant_id, code) DO UPDATE
                                         SET name           = EXCLUDED.name,
                                         timezone       = EXCLUDED.timezone,
                                         draw_time      = EXCLUDED.draw_time,
                                         sales_open_time = EXCLUDED.sales_open_time,
                                         cutoff_sec     = EXCLUDED.cutoff_sec,
                                         days_of_week   = EXCLUDED.days_of_week,
                                         active         = EXCLUDED.active,
                                         sort_order     = EXCLUDED.sort_order,
                                         flags          = EXCLUDED.flags,
                                         result_slot_id = EXCLUDED.result_slot_id,
                                         updated_at     = now(),
                                         version        = draw_channel.version + 1
                                     WHERE draw_channel.deleted_at IS NULL;

-- -----------------------------
-- E) Seed draw_channel_game
-- -----------------------------
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
         SELECT tg.id AS tenant_game_id
         FROM tenant_game tg
         JOIN game g ON g.id = tg.game_id
          WHERE g.code IN ('HT_BOLET','HT_NUMERO','HT_MARYAJ','HT_MARYAJ_GRATUIT','HT_LOTO3','HT_LOTO4','HT_LOTO5')
           AND tg.tenant_id = (SELECT tenant_id FROM t)
           AND tg.deleted_at IS NULL
     ),
     pairs AS (
         SELECT
             gen_random_uuid() AS id,
             (SELECT tenant_id FROM t) AS tenant_id,
             c.draw_channel_id,
             g.tenant_game_id,
             true AS enabled,
             '{}'::jsonb AS flags
         FROM channels c
                  CROSS JOIN games g
     )
INSERT INTO draw_channel_game (
  id, tenant_id, draw_channel_id, tenant_game_id,
  enabled, flags, created_at, updated_at, version
)
SELECT
    id, tenant_id, draw_channel_id, tenant_game_id,
    enabled, flags, now(), now(), 0
FROM pairs
    ON CONFLICT (tenant_id, draw_channel_id, tenant_game_id) DO UPDATE
                                                             SET enabled    = EXCLUDED.enabled,
                                                             flags      = EXCLUDED.flags,
                                                             updated_at = now(),
                                                             version    = draw_channel_game.version + 1
                                                         WHERE draw_channel_game.deleted_at IS NULL;

-- Reset RLS context
SELECT set_config('app.current_tenant', '', true);
SELECT set_config('app.deleted_visibility', 'active', true);
SELECT set_config('app.api_scope', '', true);
SELECT set_config('app.is_super_admin', 'false', true);
