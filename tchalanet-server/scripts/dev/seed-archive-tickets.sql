-- Dev-only archive fixture: old tickets for archive-execution-v1.
--
-- Purpose:
--   Seed a small January 2025 sales dataset so /platform/archive/runs can be
--   tested before the app has real ticket volume.
--
-- Usage example:
--   docker exec -u postgres -i tchl-postgres-dev psql -d tchalanet_db < scripts/dev/seed-archive-tickets.sql

BEGIN;

SELECT set_config('app.api_scope', 'PLATFORM', true);
SELECT set_config('app.is_super_admin', 'true', true);
SELECT set_config('app.current_tenant', '', true);
SELECT set_config('app.deleted_visibility', 'active', true);

DO $$
DECLARE
  v_tenant_id uuid := '00000000-0000-0000-0000-000000000003'::uuid;
  v_channel_id uuid;
  v_draw_id uuid := 'aaaaaaaa-0000-0000-0000-000000000101'::uuid;
  v_draw_date date := DATE '2025-01-15';
BEGIN
  SELECT id
    INTO v_channel_id
    FROM draw_channel
   WHERE tenant_id = v_tenant_id
     AND deleted_at IS NULL
   ORDER BY sort_order, code
   LIMIT 1;

  IF v_channel_id IS NULL THEN
    RAISE EXCEPTION 'No draw_channel found for tenant %. Run Flyway seeds first.', v_tenant_id;
  END IF;

  INSERT INTO draw (
    id, tenant_id, draw_channel_id, draw_date, scheduled_at, cutoff_at,
    opened_at, closed_at, status, system_generated, locked, created_at, updated_at, version
  )
  VALUES (
    v_draw_id, v_tenant_id, v_channel_id, v_draw_date,
    TIMESTAMPTZ '2025-01-15 20:00:00+00',
    TIMESTAMPTZ '2025-01-15 19:55:00+00',
    TIMESTAMPTZ '2025-01-15 00:00:00+00',
    TIMESTAMPTZ '2025-01-15 19:55:00+00',
    'CLOSED', true, false, now(), now(), 0
  )
  ON CONFLICT (id) DO UPDATE
     SET status = EXCLUDED.status,
         updated_at = now();

  DELETE FROM sales_ticket_charge
   WHERE sales_ticket_id IN (
     'aaaaaaaa-0000-0000-0000-000000001001'::uuid,
     'aaaaaaaa-0000-0000-0000-000000001002'::uuid,
     'aaaaaaaa-0000-0000-0000-000000001003'::uuid
   );

  DELETE FROM sales_ticket_line
   WHERE ticket_id IN (
     'aaaaaaaa-0000-0000-0000-000000001001'::uuid,
     'aaaaaaaa-0000-0000-0000-000000001002'::uuid,
     'aaaaaaaa-0000-0000-0000-000000001003'::uuid
   );

  DELETE FROM sales_ticket
   WHERE id IN (
     'aaaaaaaa-0000-0000-0000-000000001001'::uuid,
     'aaaaaaaa-0000-0000-0000-000000001002'::uuid,
     'aaaaaaaa-0000-0000-0000-000000001003'::uuid
   );

  INSERT INTO sales_ticket (
    id, tenant_id, seller_terminal_id, draw_id, draw_channel_id,
    ticket_code, public_code, verification_code, currency,
    stake_amount, total_amount, potential_payout_amount, winning_amount,
    sale_status, sold_at, placed_at, result_status, settlement_status,
    sale_channel, print_status, print_count, created_at, updated_at, version
  )
  VALUES
    (
      'aaaaaaaa-0000-0000-0000-000000001001', v_tenant_id, NULL, v_draw_id, v_channel_id,
      'ARCH-TCK-202501-001', 'ARCHPUB001', 'ARCHVER001', 'USD',
      20.0000, 21.0000, 180.0000, 0.0000,
      'APPROVED', TIMESTAMPTZ '2025-01-15 15:01:00+00', TIMESTAMPTZ '2025-01-15 15:01:00+00',
      'NOT_RESULTED', 'NOT_SETTLED', 'POS', 'PRINTED', 1, now(), now(), 0
    ),
    (
      'aaaaaaaa-0000-0000-0000-000000001002', v_tenant_id, NULL, v_draw_id, v_channel_id,
      'ARCH-TCK-202501-002', 'ARCHPUB002', 'ARCHVER002', 'USD',
      15.0000, 16.0000, 135.0000, 0.0000,
      'APPROVED', TIMESTAMPTZ '2025-01-16 16:02:00+00', TIMESTAMPTZ '2025-01-16 16:02:00+00',
      'NOT_RESULTED', 'NOT_SETTLED', 'POS', 'PRINTED', 1, now(), now(), 0
    ),
    (
      'aaaaaaaa-0000-0000-0000-000000001003', v_tenant_id, NULL, v_draw_id, v_channel_id,
      'ARCH-TCK-202501-003', 'ARCHPUB003', 'ARCHVER003', 'USD',
      30.0000, 31.0000, 270.0000, 0.0000,
      'APPROVED', TIMESTAMPTZ '2025-01-17 17:03:00+00', TIMESTAMPTZ '2025-01-17 17:03:00+00',
      'NOT_RESULTED', 'NOT_SETTLED', 'POS', 'PRINTED', 1, now(), now(), 0
    );

  INSERT INTO sales_ticket_line (
    id, tenant_id, ticket_id, draw_id, line_number, game_code, bet_type, bet_option,
    selection_key, display_selection, stake_amount, payout_base_amount, odds_snapshot,
    potential_payout_amount, origin, pricing_source, selection_source, result_status,
    payout_amount, created_at, updated_at, version
  )
  VALUES
    ('aaaaaaaa-0000-0000-0000-000000002001', v_tenant_id, 'aaaaaaaa-0000-0000-0000-000000001001', v_draw_id, 1, 'HT_BOLET', 'MATCH_1_2D', 1, '12', '12', 10.0000, 10.0000, 9.000000, 90.0000, 'CUSTOMER', 'STANDARD', 'CUSTOMER_SELECTED', 'PENDING', 0.0000, now(), now(), 0),
    ('aaaaaaaa-0000-0000-0000-000000002002', v_tenant_id, 'aaaaaaaa-0000-0000-0000-000000001001', v_draw_id, 2, 'HT_BOLET', 'MATCH_1_2D', 1, '34', '34', 10.0000, 10.0000, 9.000000, 90.0000, 'CUSTOMER', 'STANDARD', 'CUSTOMER_SELECTED', 'PENDING', 0.0000, now(), now(), 0),
    ('aaaaaaaa-0000-0000-0000-000000002003', v_tenant_id, 'aaaaaaaa-0000-0000-0000-000000001002', v_draw_id, 1, 'HT_BOLET', 'MATCH_1_2D', 1, '56', '56', 5.0000, 5.0000, 9.000000, 45.0000, 'CUSTOMER', 'STANDARD', 'CUSTOMER_SELECTED', 'PENDING', 0.0000, now(), now(), 0),
    ('aaaaaaaa-0000-0000-0000-000000002004', v_tenant_id, 'aaaaaaaa-0000-0000-0000-000000001002', v_draw_id, 2, 'HT_BOLET', 'MATCH_1_2D', 1, '78', '78', 10.0000, 10.0000, 9.000000, 90.0000, 'CUSTOMER', 'STANDARD', 'CUSTOMER_SELECTED', 'PENDING', 0.0000, now(), now(), 0),
    ('aaaaaaaa-0000-0000-0000-000000002005', v_tenant_id, 'aaaaaaaa-0000-0000-0000-000000001003', v_draw_id, 1, 'HT_BOLET', 'MATCH_1_2D', 1, '90', '90', 15.0000, 15.0000, 9.000000, 135.0000, 'CUSTOMER', 'STANDARD', 'CUSTOMER_SELECTED', 'PENDING', 0.0000, now(), now(), 0),
    ('aaaaaaaa-0000-0000-0000-000000002006', v_tenant_id, 'aaaaaaaa-0000-0000-0000-000000001003', v_draw_id, 2, 'HT_BOLET', 'MATCH_1_2D', 1, '11', '11', 15.0000, 15.0000, 9.000000, 135.0000, 'CUSTOMER', 'STANDARD', 'CUSTOMER_SELECTED', 'PENDING', 0.0000, now(), now(), 0);

  INSERT INTO sales_ticket_charge (
    id, tenant_id, sales_ticket_id, charge_type, paid_by, amount, currency, created_at, updated_at, version
  )
  VALUES
    ('aaaaaaaa-0000-0000-0000-000000003001', v_tenant_id, 'aaaaaaaa-0000-0000-0000-000000001001', 'SMS_DELIVERY', 'BUYER', 1.0000, 'USD', now(), now(), 0),
    ('aaaaaaaa-0000-0000-0000-000000003002', v_tenant_id, 'aaaaaaaa-0000-0000-0000-000000001002', 'SMS_DELIVERY', 'BUYER', 1.0000, 'USD', now(), now(), 0),
    ('aaaaaaaa-0000-0000-0000-000000003003', v_tenant_id, 'aaaaaaaa-0000-0000-0000-000000001003', 'SMS_DELIVERY', 'BUYER', 1.0000, 'USD', now(), now(), 0);
END $$;

SELECT
  (SELECT COUNT(*) FROM sales_ticket WHERE ticket_code LIKE 'ARCH-TCK-202501-%') AS archive_seed_tickets,
  (SELECT COUNT(*) FROM sales_ticket_line WHERE ticket_id IN (
    'aaaaaaaa-0000-0000-0000-000000001001'::uuid,
    'aaaaaaaa-0000-0000-0000-000000001002'::uuid,
    'aaaaaaaa-0000-0000-0000-000000001003'::uuid
  )) AS archive_seed_lines,
  (SELECT COUNT(*) FROM sales_ticket_charge WHERE sales_ticket_id IN (
    'aaaaaaaa-0000-0000-0000-000000001001'::uuid,
    'aaaaaaaa-0000-0000-0000-000000001002'::uuid,
    'aaaaaaaa-0000-0000-0000-000000001003'::uuid
  )) AS archive_seed_charges;

COMMIT;
