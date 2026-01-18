-- V23__core_pricing_odds.sql

-- pricing_odds: pricing snapshot per tenant/game/bet
CREATE TABLE IF NOT EXISTS pricing_odds (
                                            id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    version bigint NOT NULL DEFAULT 0,

    tenant_id uuid NOT NULL REFERENCES tenant(id),

    game_code varchar(32) NOT NULL,                 -- snapshot-friendly key
    bet_type varchar(32) NOT NULL,                  -- enum name (BetType)

-- new: optional bet option for pattern variants (1,2,3 as examples)
    bet_option smallint NULL,

    odds numeric(12,4) NOT NULL CHECK (odds > 0),

    active boolean NOT NULL DEFAULT true,

    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    deleted_at timestamptz
    );

-- sanity checks
ALTER TABLE pricing_odds
    ADD CONSTRAINT IF NOT EXISTS chk_pricing_odds_pos CHECK (odds > 0);

ALTER TABLE pricing_odds
    ADD CONSTRAINT IF NOT EXISTS chk_pricing_odds_bet_option CHECK (
    (bet_type in ('LOTTO4_PATTERN','LOTTO5_PATTERN') AND bet_option in (1,2,3))
    OR
    (bet_type NOT IN ('LOTTO4_PATTERN','LOTTO5_PATTERN') AND bet_option IS NULL)
    );

-- one active row per (tenant, game_code, bet_type, bet_option)
CREATE UNIQUE INDEX IF NOT EXISTS ux_pricing_odds_active
    ON pricing_odds(tenant_id, game_code, bet_type, bet_option)
    WHERE deleted_at IS NULL AND active = true;

CREATE INDEX IF NOT EXISTS ix_pricing_odds_lookup
    ON pricing_odds (tenant_id, game_code, bet_type, bet_option)
    WHERE active = true AND deleted_at IS NULL;

-- ticket table (core sales)
CREATE TABLE IF NOT EXISTS ticket (
                                      id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    version bigint NOT NULL DEFAULT 0,

    tenant_id uuid NOT NULL REFERENCES tenant(id),
    terminal_id uuid NOT NULL,
    draw_id uuid NOT NULL,
    session_id uuid NULL,

    ticket_code varchar(64) NOT NULL,
    public_code varchar(32) NULL,

    -- decoupled statuses
    sale_status varchar(24) NOT NULL,
    result_status varchar(24) NOT NULL DEFAULT 'NOT_RESULTED',
    settlement_status varchar(24) NOT NULL DEFAULT 'UNSETTLED',

    currency varchar(8) NOT NULL,

    total_amount numeric(14,2) NOT NULL,
    winning_amount numeric(14,2) NULL,
    resulted_at timestamptz NULL,

    approval_request_id uuid NULL,

    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    deleted_at timestamptz NULL,

    -- constraints
    CONSTRAINT uq_ticket_tenant_code UNIQUE (tenant_id, ticket_code),
    CONSTRAINT uq_ticket_public_code UNIQUE (public_code),

    CONSTRAINT chk_ticket_total_amount_nonneg CHECK (total_amount >= 0),
    CONSTRAINT chk_ticket_winning_amount_nonneg CHECK (winning_amount IS NULL OR winning_amount >= 0),

    CONSTRAINT chk_ticket_resulted_fields CHECK (
(result_status = 'NOT_RESULTED' AND winning_amount IS NULL AND resulted_at IS NULL)
    OR
(result_status IN ('WON','LOST','OVERRIDDEN') AND winning_amount IS NOT NULL AND resulted_at IS NOT NULL)
    ),

    CONSTRAINT chk_ticket_settlement_requires_result CHECK (
(settlement_status = 'UNSETTLED')
    OR
(settlement_status = 'SETTLED' AND result_status IN ('WON','LOST','OVERRIDDEN'))
    )
    );

CREATE INDEX IF NOT EXISTS ix_ticket_tenant_created_at ON ticket (tenant_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_ticket_tenant_draw_id ON ticket (tenant_id, draw_id);
CREATE INDEX IF NOT EXISTS ix_ticket_tenant_sale_status ON ticket (tenant_id, sale_status);
CREATE INDEX IF NOT EXISTS ix_ticket_tenant_result_status ON ticket (tenant_id, result_status);

-- ticket_line table
CREATE TABLE IF NOT EXISTS ticket_line (
                                           id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    version bigint NOT NULL DEFAULT 0,

    tenant_id uuid NOT NULL,
    ticket_id uuid NOT NULL REFERENCES ticket(id) ON DELETE CASCADE,

    game_code varchar(32) NOT NULL,
    bet_type varchar(32) NOT NULL,

    -- new
    bet_option smallint NULL,

    selection varchar(32) NOT NULL,
    stake numeric(12,2) NOT NULL,
    odds_snapshot numeric(12,4) NOT NULL,
    potential_payout numeric(14,2) NOT NULL,

    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    deleted_at timestamptz NULL,

    -- enforce tenant consistency ticket_line -> ticket
    CONSTRAINT fk_ticket_line_tenant_consistency CHECK (tenant_id IS NOT NULL),

    CONSTRAINT chk_ticket_line_stake_pos CHECK (stake > 0),
    CONSTRAINT chk_ticket_line_odds_pos CHECK (odds_snapshot > 0),
    CONSTRAINT chk_ticket_line_payout_nonneg CHECK (potential_payout >= 0),

    -- bet_option required only for Lotto patterns
    CONSTRAINT chk_ticket_line_bet_option CHECK (
    (bet_type IN ('LOTTO4_PATTERN','LOTTO5_PATTERN') AND bet_option IN (1,2,3))
    OR
(bet_type NOT IN ('LOTTO4_PATTERN','LOTTO5_PATTERN') AND bet_option IS NULL)
    )
    );

CREATE INDEX IF NOT EXISTS ix_ticket_line_ticket_id ON ticket_line (ticket_id);
CREATE INDEX IF NOT EXISTS ix_ticket_line_tenant_ticket_id ON ticket_line (tenant_id, ticket_id);
CREATE INDEX IF NOT EXISTS ix_ticket_line_tenant_game_bet ON ticket_line (tenant_id, game_code, bet_type, bet_option);

-- triggers: set_updated_at if function exists
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'set_updated_at') THEN
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_pricing_odds_set_updated_at') THEN
CREATE TRIGGER trg_pricing_odds_set_updated_at
    BEFORE UPDATE ON pricing_odds
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_ticket_set_updated_at') THEN
CREATE TRIGGER trg_ticket_set_updated_at
    BEFORE UPDATE ON ticket
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_ticket_line_set_updated_at') THEN
CREATE TRIGGER trg_ticket_line_set_updated_at
    BEFORE UPDATE ON ticket_line
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
END IF;
END IF;
END$$;
