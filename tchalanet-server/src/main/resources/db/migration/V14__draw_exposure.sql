-- V14__draw_exposure.sql (reworked)

CREATE TABLE draw_exposure
(
    id                     uuid PRIMARY KEY        DEFAULT gen_random_uuid(),

    tenant_id              uuid           NOT NULL REFERENCES tenant (id),
    draw_id                uuid           NOT NULL REFERENCES draw (id),

    scope_type             varchar(16)    NOT NULL CHECK (scope_type IN
                                                          ('TENANT', 'AGENT', 'TERMINAL', 'OUTLET', 'ZONE',
                                                           'DRAWCHANNEL')),
    scope_id               uuid           NOT NULL, -- for TENANT use tenant_id

    bet_type               varchar(32)    NOT NULL,
    selection_key          varchar(64)    NOT NULL,

    stake_total            numeric(14, 2) NOT NULL DEFAULT 0,
    sales_count            bigint         NOT NULL DEFAULT 0,
    potential_payout_total numeric(14, 2) NOT NULL DEFAULT 0,

    -- Audit columns
    created_at             timestamptz    NOT NULL DEFAULT now(),
    updated_at             timestamptz    NOT NULL DEFAULT now(),
    created_by             uuid NULL,               -- user_id/agent_id if you have it (nullable for system projector)
    updated_by             uuid NULL,
    deleted_at             timestamptz NULL,

    -- Event trace (important for support/idempotence debugging)
    last_event_id          uuid NULL,
    last_event_at          timestamptz NULL,

    -- Optional version counter (useful for debugging / replays)
    version                bigint         NOT NULL DEFAULT 0,

    CONSTRAINT uq_draw_exposure_key UNIQUE (tenant_id, draw_id, scope_type, scope_id, bet_type, selection_key)
);

-- Reads for evaluation/facts
CREATE INDEX ix_draw_exposure_lookup
    ON draw_exposure (draw_id, scope_type, scope_id, bet_type, selection_key);

-- Dashboard "top stake / payout" (no tenant filter in code; RLS applies)
CREATE INDEX ix_draw_exposure_top_stake
    ON draw_exposure (draw_id, scope_type, scope_id, stake_total DESC);

CREATE INDEX ix_draw_exposure_top_payout
    ON draw_exposure (draw_id, scope_type, scope_id, potential_payout_total DESC);

CREATE OR REPLACE FUNCTION increment_draw_exposure(
  p_tenant_id uuid,
  p_draw_id uuid,
  p_scope_type varchar,
  p_scope_id uuid,
  p_bet_type varchar,
  p_selection_key varchar,
  p_stake_delta numeric,
  p_sales_delta bigint,
  p_payout_delta numeric,
  p_event_id uuid,
  p_event_at timestamptz,
  p_actor_id uuid
) RETURNS void AS $$
BEGIN
INSERT INTO draw_exposure (
    tenant_id, draw_id, scope_type, scope_id,
    bet_type, selection_key,
    stake_total, sales_count, potential_payout_total,
    created_at, updated_at, created_by, updated_by, deleted_at,
    last_event_id, last_event_at, version
)
VALUES (
           p_tenant_id, p_draw_id, p_scope_type, p_scope_id,
           p_bet_type, p_selection_key,
           p_stake_delta, p_sales_delta, p_payout_delta,
           now(), now(), p_actor_id, p_actor_id, NULL,
           p_event_id, p_event_at, 0
       )
    ON CONFLICT (tenant_id, draw_id, scope_type, scope_id, bet_type, selection_key)
  DO UPDATE SET
    stake_total = draw_exposure.stake_total + p_stake_delta,
             sales_count = draw_exposure.sales_count + p_sales_delta,
             potential_payout_total = draw_exposure.potential_payout_total + p_payout_delta,
             updated_at = now(),
             updated_by = p_actor_id,
             last_event_id = p_event_id,
             last_event_at = p_event_at,
             version = draw_exposure.version + 1;
END;
$$ LANGUAGE plpgsql;
