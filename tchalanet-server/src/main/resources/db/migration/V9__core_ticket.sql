-- ============================================================
-- CORE PRICING + SALES (FRESH DATABASE)
-- ============================================================

-- ============================================================
-- PRICING_ODDS
-- ============================================================

CREATE TABLE pricing_odds (
                              id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                              version bigint NOT NULL DEFAULT 0,

                              tenant_id uuid NOT NULL REFERENCES tenant(id),

                              game_code varchar(32) NOT NULL,

                              bet_type varchar(32) NOT NULL CHECK (
                                  bet_type IN (
                                               'MATCH_1_2D','MATCH_2_2D','MATCH_3_2D',
                                               'LOTTO3_3D','MARRIAGE_2D2D',
                                               'LOTTO4_PATTERN','LOTTO5_PATTERN'
                                      )
                                  ),

                              bet_option smallint NULL,

                              odds numeric(12,4) NOT NULL CHECK (odds > 0),

                              active boolean NOT NULL DEFAULT true,

                              created_at timestamptz NOT NULL DEFAULT now(),
                              created_by uuid,
                              updated_at timestamptz NOT NULL DEFAULT now(),
                              updated_by uuid,
                              deleted_at timestamptz,

                              CONSTRAINT chk_pricing_odds_bet_option CHECK (
                                  (bet_type IN ('LOTTO4_PATTERN','LOTTO5_PATTERN') AND bet_option IN (1,2,3))
                                      OR
                                  (bet_type NOT IN ('LOTTO4_PATTERN','LOTTO5_PATTERN') AND bet_option IS NULL)
                                  )
);

-- unique active snapshot per tenant/game/bet
CREATE UNIQUE INDEX ux_pricing_odds_active
    ON pricing_odds (tenant_id, game_code, bet_type, COALESCE(bet_option, -1))
    WHERE deleted_at IS NULL AND active = true;

CREATE INDEX ix_pricing_odds_lookup
    ON pricing_odds (tenant_id, game_code, bet_type, COALESCE(bet_option, -1))
    WHERE deleted_at IS NULL AND active = true;


-- ============================================================
-- TICKET
-- ============================================================

CREATE TABLE ticket (
                        id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                        version bigint NOT NULL DEFAULT 0,

                        tenant_id uuid NOT NULL REFERENCES tenant(id),

                        terminal_id uuid NOT NULL,
                        draw_id uuid NOT NULL,
                        session_id uuid NULL,

                        ticket_code varchar(64) NOT NULL,
                        public_code varchar(32) NOT NULL,

                        sale_status varchar(24) NOT NULL CHECK (
                            sale_status IN ('SOLD','PENDING_APPROVAL','VOID','REJECTED')
                            ),

                        result_status varchar(24) NOT NULL DEFAULT 'NOT_RESULTED' CHECK (
                            result_status IN ('NOT_RESULTED','WON','LOST','OVERRIDDEN')
                            ),

                        settlement_status varchar(24) NOT NULL DEFAULT 'UNSETTLED' CHECK (
                            settlement_status IN ('UNSETTLED','SETTLED')
                            ),

                        currency varchar(8) NOT NULL,

                        total_amount numeric(14,2) NOT NULL CHECK (total_amount >= 0),
                        winning_amount numeric(14,2) NULL CHECK (winning_amount IS NULL OR winning_amount >= 0),

                        resulted_at timestamptz NULL,

                        approval_request_id uuid NULL,

                        created_at timestamptz NOT NULL DEFAULT now(),
                        created_by uuid,
                        updated_at timestamptz NOT NULL DEFAULT now(),
                        updated_by uuid,
                        deleted_at timestamptz,

    -- unique codes
                        CONSTRAINT uq_ticket_tenant_code UNIQUE (tenant_id, ticket_code),
                        CONSTRAINT uq_ticket_public_code UNIQUE (public_code),

    -- settlement logic coherence
                        CONSTRAINT chk_ticket_result_fields CHECK (
                            (result_status = 'NOT_RESULTED' AND winning_amount IS NULL AND resulted_at IS NULL)
                                OR
                            (result_status IN ('WON','LOST','OVERRIDDEN')
                                AND winning_amount IS NOT NULL
                                AND resulted_at IS NOT NULL)
                            ),

                        CONSTRAINT chk_ticket_settlement_requires_result CHECK (
                            settlement_status = 'UNSETTLED'
                                OR
                            (settlement_status = 'SETTLED'
                                AND result_status IN ('WON','LOST','OVERRIDDEN'))
                            ),

    -- needed for composite FK in ticket_line
                        CONSTRAINT uq_ticket_id_tenant UNIQUE (id, tenant_id)
);

CREATE INDEX ix_ticket_tenant_created_at
    ON ticket (tenant_id, created_at DESC);

CREATE INDEX ix_ticket_tenant_draw_id
    ON ticket (tenant_id, draw_id);

CREATE INDEX ix_ticket_tenant_sale_status
    ON ticket (tenant_id, sale_status);

CREATE INDEX ix_ticket_tenant_result_status
    ON ticket (tenant_id, result_status);


-- ============================================================
-- TICKET_LINE
-- ============================================================

CREATE TABLE ticket_line (
                             id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                             version bigint NOT NULL DEFAULT 0,

                             tenant_id uuid NOT NULL,
                             ticket_id uuid NOT NULL,

                             game_code varchar(32) NOT NULL,

                             bet_type varchar(32) NOT NULL CHECK (
                                 bet_type IN (
                                              'MATCH_1_2D','MATCH_2_2D','MATCH_3_2D',
                                              'LOTTO3_3D','MARRIAGE_2D2D',
                                              'LOTTO4_PATTERN','LOTTO5_PATTERN'
                                     )
                                 ),

                             bet_option smallint NULL,

                             selection varchar(32) NOT NULL,

                             stake numeric(12,2) NOT NULL CHECK (stake > 0),
                             odds_snapshot numeric(12,4) NOT NULL CHECK (odds_snapshot > 0),
                             potential_payout numeric(14,2) NOT NULL CHECK (potential_payout >= 0),

                             created_at timestamptz NOT NULL DEFAULT now(),
                             created_by uuid,
                             updated_at timestamptz NOT NULL DEFAULT now(),
                             updated_by uuid,
                             deleted_at timestamptz,

                             CONSTRAINT chk_ticket_line_bet_option CHECK (
                                 (bet_type IN ('LOTTO4_PATTERN','LOTTO5_PATTERN') AND bet_option IN (1,2,3))
                                     OR
                                 (bet_type NOT IN ('LOTTO4_PATTERN','LOTTO5_PATTERN') AND bet_option IS NULL)
                                 ),

    -- strict tenant consistency
                             CONSTRAINT fk_ticket_line_ticket
                                 FOREIGN KEY (ticket_id, tenant_id)
                                     REFERENCES ticket (id, tenant_id)
                                     ON DELETE CASCADE
);

CREATE INDEX ix_ticket_line_ticket
    ON ticket_line (ticket_id);

CREATE INDEX ix_ticket_line_lookup
    ON ticket_line (tenant_id, game_code, bet_type, COALESCE(bet_option, -1));


-- ============================================================
-- TICKET_SETTLEMENT (IDEMPOTENCE GUARD)
-- ============================================================

CREATE TABLE ticket_settlement (
                                   id uuid PRIMARY KEY DEFAULT gen_random_uuid(),

                                   tenant_id uuid NOT NULL REFERENCES tenant(id),
                                   ticket_id uuid NOT NULL,
                                   draw_result_id uuid NOT NULL,

                                   created_at timestamptz NOT NULL DEFAULT now(),

                                   CONSTRAINT uq_ticket_settlement UNIQUE (tenant_id, ticket_id, draw_result_id),

                                   CONSTRAINT fk_ticket_settlement_ticket
                                       FOREIGN KEY (ticket_id, tenant_id)
                                           REFERENCES ticket (id, tenant_id)
                                           ON DELETE CASCADE
);

CREATE INDEX ix_ticket_settlement_draw
    ON ticket_settlement (tenant_id, draw_result_id);
