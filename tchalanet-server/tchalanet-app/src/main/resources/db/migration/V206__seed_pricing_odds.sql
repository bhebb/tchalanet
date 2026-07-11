-- V38: seed pricing_odds by pricing_variant_code
-- Notes:
--  - multiplier mapped to odds (numeric(12,4))
-- Flyway RLS context for default tenant seed
SELECT set_config('app.current_tenant', '00000000-0000-0000-0000-000000000003', true);
SELECT set_config('app.deleted_visibility', 'active', true);
SELECT set_config('app.api_scope', 'tenant', true);
SELECT set_config('app.is_super_admin', 'false', true);

INSERT INTO pricing_odds (
    tenant_id,
    game_code,
    pricing_variant_code,
    bet_type,
    bet_option,
    odds,
    payout_rule_type,
    fixed_amount
)
VALUES
    ('00000000-0000-0000-0000-000000000003'::uuid, 'HT_BOLET',   'MATCH_1_2D',              'MATCH_1_2D',      NULL, 50.0000, 'STAKE_MULTIPLIER', NULL),
    ('00000000-0000-0000-0000-000000000003'::uuid, 'HT_BOLET',   'MATCH_2_2D',              'MATCH_2_2D',      NULL, 20.0000, 'STAKE_MULTIPLIER', NULL),
    ('00000000-0000-0000-0000-000000000003'::uuid, 'HT_BOLET',   'MATCH_3_2D',              'MATCH_3_2D',      NULL, 10.0000, 'STAKE_MULTIPLIER', NULL),

    ('00000000-0000-0000-0000-000000000003'::uuid, 'HT_MARYAJ',  'MARRIAGE_EXACT_ORDER',    'MARRIAGE_2D2D',   1,    1000.0000, 'STAKE_MULTIPLIER', NULL),
    ('00000000-0000-0000-0000-000000000003'::uuid, 'HT_MARYAJ',  'MARRIAGE_REVERSE_ALLOWED','MARRIAGE_2D2D',   2,    1000.0000, 'STAKE_MULTIPLIER', NULL),

    -- Promotional Maryaj: free customer stake, realized payout uses a fixed amount snapshot.
    ('00000000-0000-0000-0000-000000000003'::uuid, 'HT_MARYAJ_GRATIS', 'MARRIAGE_EXACT_ORDER',     'MARRIAGE_2D2D', 1, 1.0000, 'FIXED_AMOUNT', 500.0000),
    ('00000000-0000-0000-0000-000000000003'::uuid, 'HT_MARYAJ_GRATIS', 'MARRIAGE_REVERSE_ALLOWED', 'MARRIAGE_2D2D', 2, 1.0000, 'FIXED_AMOUNT', 500.0000),

    ('00000000-0000-0000-0000-000000000003'::uuid, 'HT_LOTO3',   'LOTTO3_STRAIGHT',         'LOTTO3_3D',       1,    500.0000, 'STAKE_MULTIPLIER', NULL),
    ('00000000-0000-0000-0000-000000000003'::uuid, 'HT_LOTO3',   'LOTTO3_BOX_3_WAY',        'LOTTO3_3D',       2,    500.0000, 'STAKE_MULTIPLIER', NULL),
    ('00000000-0000-0000-0000-000000000003'::uuid, 'HT_LOTO3',   'LOTTO3_BOX_6_WAY',        'LOTTO3_3D',       2,    500.0000, 'STAKE_MULTIPLIER', NULL),

    ('00000000-0000-0000-0000-000000000003'::uuid, 'HT_LOTO4',   'LOTTO4_STRAIGHT',         'LOTTO4_PATTERN',  1,    5000.0000, 'STAKE_MULTIPLIER', NULL),
    ('00000000-0000-0000-0000-000000000003'::uuid, 'HT_LOTO4',   'LOTTO4_BOX_4_WAY',        'LOTTO4_PATTERN',  2,    1200.0000, 'STAKE_MULTIPLIER', NULL),
    ('00000000-0000-0000-0000-000000000003'::uuid, 'HT_LOTO4',   'LOTTO4_BOX_6_WAY',        'LOTTO4_PATTERN',  2,    800.0000, 'STAKE_MULTIPLIER', NULL),
    ('00000000-0000-0000-0000-000000000003'::uuid, 'HT_LOTO4',   'LOTTO4_BOX_12_WAY',       'LOTTO4_PATTERN',  2,    400.0000, 'STAKE_MULTIPLIER', NULL),
    ('00000000-0000-0000-0000-000000000003'::uuid, 'HT_LOTO4',   'LOTTO4_BOX_24_WAY',       'LOTTO4_PATTERN',  2,    200.0000, 'STAKE_MULTIPLIER', NULL),
    ('00000000-0000-0000-0000-000000000003'::uuid, 'HT_LOTO4',   'LOTTO4_FRONT_PAIR',       'LOTTO4_PATTERN',  3,    5000.0000, 'STAKE_MULTIPLIER', NULL),
    ('00000000-0000-0000-0000-000000000003'::uuid, 'HT_LOTO4',   'LOTTO4_BACK_PAIR',        'LOTTO4_PATTERN',  4,    5000.0000, 'STAKE_MULTIPLIER', NULL),

    ('00000000-0000-0000-0000-000000000003'::uuid, 'HT_LOTO5',   'LOTTO5_LOT1_LOT2',        'LOTTO5_PATTERN',  1,    25000.0000, 'STAKE_MULTIPLIER', NULL),
    ('00000000-0000-0000-0000-000000000003'::uuid, 'HT_LOTO5',   'LOTTO5_LOT1_LOT3',        'LOTTO5_PATTERN',  2,    25000.0000, 'STAKE_MULTIPLIER', NULL),
    ('00000000-0000-0000-0000-000000000003'::uuid, 'HT_LOTO5',   'LOTTO5_MIXED_1_2_3',      'LOTTO5_PATTERN',  3,    25000.0000, 'STAKE_MULTIPLIER', NULL)
    ON CONFLICT (tenant_id, game_code, pricing_variant_code) DO NOTHING;

-- Reset RLS context
SELECT set_config('app.current_tenant', '', true);
SELECT set_config('app.deleted_visibility', 'active', true);
SELECT set_config('app.api_scope', '', true);
SELECT set_config('app.is_super_admin', 'false', true);
