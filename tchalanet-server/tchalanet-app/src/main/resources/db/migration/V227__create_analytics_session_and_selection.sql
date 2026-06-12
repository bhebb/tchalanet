-- V227 — analytics_session + analytics_selection (data-lifecycle-archive-v1 Phase 3)
--
-- analytics_session: per-session KPI row, keyed on sales_session_id.
-- analytics_selection: per-game/bet_type/selection profitability, keyed on
--   (tenant_id, ref_date, game_code, bet_type, bet_option, selection_key, draw_channel_id).
--
-- Both follow the same upsert-function pattern as analytics_daily (V109).
-- RLS: enabled; no row-level policy (application-level tenant filter).

-- ────────────────────────────────────────────────
-- analytics_session
-- ────────────────────────────────────────────────
CREATE TABLE analytics_session
(
    id                   uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    sales_session_id     uuid        NOT NULL UNIQUE,
    tenant_id            uuid        NOT NULL REFERENCES tenant (id),
    outlet_id            uuid        NULL,
    terminal_id          uuid        NULL,
    seller_user_id       uuid        NULL,
    business_date        date        NOT NULL,

    tickets_sold_count   bigint      NOT NULL DEFAULT 0,
    tickets_voided_count bigint      NOT NULL DEFAULT 0,
    gross_sales_cents    bigint      NOT NULL DEFAULT 0,
    stake_total_cents    bigint      NOT NULL DEFAULT 0,
    payouts_paid_cents   bigint      NOT NULL DEFAULT 0,

    status               varchar(32) NOT NULL DEFAULT 'OPEN',
    opened_at            timestamptz NULL,
    closed_at            timestamptz NULL,

    version              bigint      NOT NULL DEFAULT 0,
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX ix_analytics_session__tenant_date
    ON analytics_session (tenant_id, business_date);
CREATE INDEX ix_analytics_session__outlet_date
    ON analytics_session (outlet_id, business_date)
    WHERE outlet_id IS NOT NULL;
CREATE INDEX ix_analytics_session__seller_date
    ON analytics_session (seller_user_id, business_date)
    WHERE seller_user_id IS NOT NULL;

CREATE TRIGGER trg_analytics_session__set_updated_at
    BEFORE UPDATE ON analytics_session
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

ALTER TABLE analytics_session ENABLE ROW LEVEL SECURITY;
GRANT SELECT, INSERT, UPDATE ON analytics_session TO app_user;

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

-- Session: ensure-or-open row
CREATE OR REPLACE FUNCTION public.ensure_analytics_session(
    p_session_id     uuid,
    p_tenant_id      uuid,
    p_outlet_id      uuid,
    p_terminal_id    uuid,
    p_seller_user_id uuid,
    p_business_date  date,
    p_opened_at      timestamptz
) RETURNS void AS
$$
BEGIN
    INSERT INTO analytics_session
        (sales_session_id, tenant_id, outlet_id, terminal_id,
         seller_user_id, business_date, status, opened_at)
    VALUES (p_session_id, p_tenant_id, p_outlet_id, p_terminal_id,
            p_seller_user_id, p_business_date, 'OPEN', p_opened_at)
    ON CONFLICT (sales_session_id) DO NOTHING;
END;
$$ LANGUAGE plpgsql;

-- Session: close
CREATE OR REPLACE FUNCTION public.close_analytics_session(
    p_session_id uuid,
    p_closed_at  timestamptz
) RETURNS void AS
$$
BEGIN
    UPDATE analytics_session
    SET status    = 'CLOSED',
        closed_at = p_closed_at
    WHERE sales_session_id = p_session_id
      AND status = 'OPEN';
END;
$$ LANGUAGE plpgsql;

-- Session: increment ticket counters
CREATE OR REPLACE FUNCTION public.increment_analytics_session(
    p_session_id         uuid,
    p_tickets_sold_delta  bigint,
    p_tickets_voided_delta bigint,
    p_gross_sales_delta  bigint,
    p_stake_total_delta  bigint,
    p_payouts_paid_delta bigint
) RETURNS void AS
$$
BEGIN
    UPDATE analytics_session
    SET tickets_sold_count   = tickets_sold_count + p_tickets_sold_delta,
        tickets_voided_count = tickets_voided_count + p_tickets_voided_delta,
        gross_sales_cents    = gross_sales_cents + p_gross_sales_delta,
        stake_total_cents    = stake_total_cents + p_stake_total_delta,
        payouts_paid_cents   = payouts_paid_cents + p_payouts_paid_delta
    WHERE sales_session_id = p_session_id;
END;
$$ LANGUAGE plpgsql;

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
