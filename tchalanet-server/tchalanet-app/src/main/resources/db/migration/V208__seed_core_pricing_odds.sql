-- V38: seed pricing_odds adapted to existing schema
-- Notes:
--  - multiplier mapped to odds (numeric(12,4))
-- Flyway RLS context for default tenant seed
SELECT set_config('app.current_tenant', '00000000-0000-0000-0000-000000000003', true);
SELECT set_config('app.deleted_visibility', 'active', true);
SELECT set_config('app.api_scope', 'tenant', true);
SELECT set_config('app.is_super_admin', 'false', true);

INSERT INTO pricing_odds (tenant_id, game_code, bet_type, bet_option, odds)
VALUES
    ('00000000-0000-0000-0000-000000000003'::uuid, 'HT_BOLET',   'MATCH_1_2D',    NULL, 50.0000),
    ('00000000-0000-0000-0000-000000000003'::uuid, 'HT_BOLET',   'MATCH_2_2D',    NULL, 20.0000),
    ('00000000-0000-0000-0000-000000000003'::uuid, 'HT_BOLET',   'MATCH_3_2D',    NULL, 10.0000),

    ('00000000-0000-0000-0000-000000000003'::uuid, 'HT_MARYAJ',  'MARRIAGE_2D2D', NULL, 1000.0000),

    -- Promotional Maryaj: free customer stake, payout uses TicketLine.payoutBaseAmount × odds.
    -- Example: payoutBaseAmount=50, odds=10 => potential payout=500.
    ('00000000-0000-0000-0000-000000000003'::uuid, 'HT_MARYAJ_GRATUIT', 'MARRIAGE_2D2D', NULL, 10.0000),

    ('00000000-0000-0000-0000-000000000003'::uuid, 'HT_LOTO3',   'LOTTO3_3D',     NULL, 500.0000),

    ('00000000-0000-0000-0000-000000000003'::uuid, 'HT_LOTO4',   'LOTTO4_PATTERN', 1, 5000.0000),
    ('00000000-0000-0000-0000-000000000003'::uuid, 'HT_LOTO4',   'LOTTO4_PATTERN', 2, 5000.0000),
    ('00000000-0000-0000-0000-000000000003'::uuid, 'HT_LOTO4',   'LOTTO4_PATTERN', 3, 5000.0000),

    ('00000000-0000-0000-0000-000000000003'::uuid, 'HT_LOTO5',   'LOTTO5_PATTERN', 1, 25000.0000),
    ('00000000-0000-0000-0000-000000000003'::uuid, 'HT_LOTO5',   'LOTTO5_PATTERN', 2, 25000.0000),
    ('00000000-0000-0000-0000-000000000003'::uuid, 'HT_LOTO5',   'LOTTO5_PATTERN', 3, 25000.0000)
    ON CONFLICT (tenant_id, game_code, bet_type, bet_option) DO NOTHING;

-- Reset RLS context
SELECT set_config('app.current_tenant', '', true);
SELECT set_config('app.deleted_visibility', 'active', true);
SELECT set_config('app.api_scope', '', true);
SELECT set_config('app.is_super_admin', 'false', true);
