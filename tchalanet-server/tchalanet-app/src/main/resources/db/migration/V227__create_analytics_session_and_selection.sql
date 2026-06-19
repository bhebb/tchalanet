-- V227 — analytics_selection (data-lifecycle-archive-v1 Phase 3)
--
-- analytics_selection: per-game/bet_type/selection profitability, keyed on
--   (tenant_id, ref_date, game_code, bet_type, bet_option, selection_key, draw_channel_id).
--
-- Follows the same upsert-function pattern as analytics_daily (V109).
-- RLS: enabled; no row-level policy (application-level tenant filter).

-- ────────────────────────────────────────────────
-- analytics_selection
-- ────────────────────────────────────────────────
CREATE TABLE analytics_selection
(
    id                        uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                 uuid         NOT NULL REFERENCES tenant (id),
    ref_date                  date         NOT NULL,
    draw_channel_id           uuid         NULL,
    game_code                 varchar(64)  NOT NULL,
    bet_type                  varchar(64)  NOT NULL,
    bet_option                smallint     NULL,
    selection_key             varchar(128) NULL,

    tickets_count             bigint       NOT NULL DEFAULT 0,
    stake_sum_cents           bigint       NOT NULL DEFAULT 0,
    winnings_calculated_cents bigint       NOT NULL DEFAULT 0,

    version                   bigint       NOT NULL DEFAULT 0,
    created_at                timestamptz  NOT NULL DEFAULT now(),
    updated_at                timestamptz  NOT NULL DEFAULT now()
);

-- Unique constraint drives ON CONFLICT upsert
CREATE UNIQUE INDEX uix_analytics_selection__natural
    ON analytics_selection (
        tenant_id,
        ref_date,
        game_code,
        bet_type,
        COALESCE(bet_option, -1),
        COALESCE(selection_key, ''),
        COALESCE(draw_channel_id, '00000000-0000-0000-0000-000000000000'::uuid)
    );

CREATE INDEX ix_analytics_selection__tenant_date
    ON analytics_selection (tenant_id, ref_date);
CREATE INDEX ix_analytics_selection__game_date
    ON analytics_selection (game_code, ref_date);

CREATE TRIGGER trg_analytics_selection__set_updated_at
    BEFORE UPDATE ON analytics_selection
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

ALTER TABLE analytics_selection ENABLE ROW LEVEL SECURITY;
GRANT SELECT, INSERT, UPDATE ON analytics_selection TO app_user;

-- ────────────────────────────────────────────────
-- Upsert helpers
-- ────────────────────────────────────────────────

-- Selection: atomic upsert-increment
CREATE OR REPLACE FUNCTION public.upsert_analytics_selection(
    p_tenant_id      uuid,
    p_ref_date       date,
    p_draw_channel_id uuid,
    p_game_code      varchar,
    p_bet_type       varchar,
    p_bet_option     smallint,
    p_selection_key  varchar,
    p_tickets_delta  bigint,
    p_stake_delta    bigint,
    p_winnings_delta bigint
) RETURNS void AS
$$
BEGIN
    INSERT INTO analytics_selection
        (tenant_id, ref_date, draw_channel_id, game_code, bet_type, bet_option,
         selection_key, tickets_count, stake_sum_cents, winnings_calculated_cents)
    VALUES (p_tenant_id, p_ref_date, p_draw_channel_id, p_game_code, p_bet_type,
            p_bet_option, p_selection_key, p_tickets_delta, p_stake_delta, p_winnings_delta)
    ON CONFLICT (
        tenant_id,
        ref_date,
        game_code,
        bet_type,
        COALESCE(bet_option, -1),
        COALESCE(selection_key, ''),
        COALESCE(draw_channel_id, '00000000-0000-0000-0000-000000000000'::uuid)
        ) DO UPDATE SET
        tickets_count             = analytics_selection.tickets_count + EXCLUDED.tickets_count,
        stake_sum_cents           = analytics_selection.stake_sum_cents + EXCLUDED.stake_sum_cents,
        winnings_calculated_cents = analytics_selection.winnings_calculated_cents +
                                    EXCLUDED.winnings_calculated_cents,
        version                   = analytics_selection.version + 1;
END;
$$ LANGUAGE plpgsql;
