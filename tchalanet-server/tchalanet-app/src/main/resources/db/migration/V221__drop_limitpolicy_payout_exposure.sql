DROP INDEX IF EXISTS idx_draw_exposure_top_payout;

DROP FUNCTION IF EXISTS public.increment_draw_exposure(
  uuid,
  uuid,
  varchar,
  uuid,
  varchar,
  varchar,
  numeric,
  numeric,
  uuid,
  timestamptz
);

CREATE OR REPLACE FUNCTION public.increment_draw_exposure(
  p_tenant_id uuid,
  p_draw_id uuid,
  p_scope_type varchar,
  p_scope_id uuid,
  p_bet_type varchar,
  p_selection_key varchar,
  p_stake numeric,
  p_event_id uuid,
  p_event_at timestamptz
) RETURNS void AS $$
BEGIN
INSERT INTO draw_exposure (
    tenant_id,
    draw_id,
    scope_type,
    scope_id,
    bet_type,
    selection_key,
    stake_total,
    sales_count,
    last_event_id,
    last_event_at
) VALUES (
             p_tenant_id,
             p_draw_id,
             p_scope_type,
             p_scope_id,
             p_bet_type,
             p_selection_key,
             p_stake,
             1,
             p_event_id,
             p_event_at
         )
    ON CONFLICT (
    tenant_id,
    draw_id,
    scope_type,
    scope_id,
    bet_type,
    selection_key
  )
WHERE deleted_at IS NULL
    DO UPDATE SET
    stake_total = draw_exposure.stake_total + EXCLUDED.stake_total,
           sales_count = draw_exposure.sales_count + 1,
           last_event_id = EXCLUDED.last_event_id,
           last_event_at = EXCLUDED.last_event_at,
           updated_at = now();
END;
$$ LANGUAGE plpgsql;

ALTER TABLE draw_exposure
  DROP CONSTRAINT IF EXISTS ck_draw_exposure_amounts_non_negative;

ALTER TABLE draw_exposure
  DROP COLUMN IF EXISTS potential_payout_total;

ALTER TABLE draw_exposure
  ADD CONSTRAINT ck_draw_exposure_amounts_non_negative CHECK (
    stake_total >= 0
    AND sales_count >= 0
  );
