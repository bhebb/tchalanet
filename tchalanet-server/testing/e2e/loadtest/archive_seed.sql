-- Archive Locust E2E fixture: deterministic January 2025 rows across archive datasets.
--
-- Run through TCH_ARCHIVE_SEED_COMMAND before Locust calls the real /platform/archive/** endpoints.

BEGIN;

SELECT set_config('app.api_scope', 'PLATFORM', true);
SELECT set_config('app.is_super_admin', 'true', true);
SELECT set_config('app.current_tenant', '', true);
SELECT set_config('app.deleted_visibility', 'active', true);

SELECT public.archive_ensure_month_partition('audit_log'::regclass, DATE '2025-01-01');

DO $$
DECLARE
  v_tenant_id uuid;
  v_draw_channel_id uuid;
  v_result_slot_id uuid;
  v_draw_id uuid := 'aaaaaaaa-1000-0000-0000-000000000101'::uuid;
  v_draw_result_id uuid := 'aaaaaaaa-1000-0000-0000-000000000201'::uuid;
  v_terminal_id uuid;
  v_rev integer := 250101;
BEGIN
  SELECT id INTO v_tenant_id FROM tenant WHERE code = 'tchalanet' ORDER BY created_at LIMIT 1;
  IF v_tenant_id IS NULL THEN
    SELECT id INTO v_tenant_id FROM tenant ORDER BY created_at LIMIT 1;
  END IF;
  IF v_tenant_id IS NULL THEN
    RAISE EXCEPTION 'Archive seed needs at least one tenant.';
  END IF;

  SELECT id INTO v_draw_channel_id
    FROM draw_channel
   WHERE tenant_id = v_tenant_id AND deleted_at IS NULL
   ORDER BY sort_order, code
   LIMIT 1;
  IF v_draw_channel_id IS NULL THEN
    RAISE EXCEPTION 'Archive seed needs at least one draw_channel for tenant %.', v_tenant_id;
  END IF;

  SELECT id INTO v_result_slot_id FROM result_slot ORDER BY sort_order, slot_key LIMIT 1;
  IF v_result_slot_id IS NULL THEN
    RAISE EXCEPTION 'Archive seed needs at least one result_slot.';
  END IF;

  SELECT id INTO v_terminal_id
    FROM seller_terminal
   WHERE tenant_id = v_tenant_id
   ORDER BY created_at
   LIMIT 1;

  DELETE FROM archive_lookup_index
   WHERE archive_object_id IN (
     SELECT id FROM archive_object
      WHERE period_start = DATE '2025-01-01' AND period_end = DATE '2025-02-01'
   );
  DELETE FROM archive_object
   WHERE period_start = DATE '2025-01-01' AND period_end = DATE '2025-02-01';
  DELETE FROM archive_run
   WHERE idempotency_key = 'run:global:2025-01-01:2025-02-01';

  DELETE FROM sales_ticket_charge
   WHERE sales_ticket_id IN (
     'aaaaaaaa-1000-0000-0000-000000001001'::uuid,
     'aaaaaaaa-1000-0000-0000-000000001002'::uuid
   );
  DELETE FROM sales_ticket_line
   WHERE ticket_id IN (
     'aaaaaaaa-1000-0000-0000-000000001001'::uuid,
     'aaaaaaaa-1000-0000-0000-000000001002'::uuid
   );
  DELETE FROM sales_ticket
   WHERE id IN (
     'aaaaaaaa-1000-0000-0000-000000001001'::uuid,
     'aaaaaaaa-1000-0000-0000-000000001002'::uuid
   );
  DELETE FROM analytics_seller_terminal_draw WHERE draw_id = v_draw_id;
  DELETE FROM analytics_draw WHERE draw_id = v_draw_id;
  DELETE FROM analytics_selection
   WHERE tenant_id = v_tenant_id AND ref_date >= DATE '2025-01-01' AND ref_date < DATE '2025-02-01';
  DELETE FROM analytics_daily
   WHERE ref_date >= DATE '2025-01-01' AND ref_date < DATE '2025-02-01';
  UPDATE draw
     SET draw_result_id = NULL
   WHERE draw_result_id = v_draw_result_id
      OR draw_result_id IN (
        SELECT id
          FROM draw_result
         WHERE result_slot_id = v_result_slot_id
           AND (
             occurred_at = TIMESTAMPTZ '2025-01-15 22:00:00+00'
             OR result_date = DATE '2025-01-15'
           )
      );
  DELETE FROM draw WHERE id = v_draw_id;
  DELETE FROM draw_result
   WHERE id = v_draw_result_id
      OR (
        result_slot_id = v_result_slot_id
        AND (
          occurred_at = TIMESTAMPTZ '2025-01-15 22:00:00+00'
          OR result_date = DATE '2025-01-15'
        )
      );
  DELETE FROM audit_log
   WHERE occurred_at >= TIMESTAMPTZ '2025-01-01 00:00:00+00'
     AND occurred_at < TIMESTAMPTZ '2025-02-01 00:00:00+00'
     AND request_id = 'locust-archive-seed';
  DELETE FROM draw_result_aud WHERE rev = v_rev;
  DELETE FROM limit_assignment_aud WHERE rev = v_rev;
  DELETE FROM seller_terminal_aud WHERE rev = v_rev;
  DELETE FROM revinfo WHERE rev = v_rev;
  DELETE FROM processed_event
   WHERE tenant_id = v_tenant_id AND handler_key LIKE 'locust.archive.seed.%';
  DELETE FROM batch.BATCH_STEP_EXECUTION_CONTEXT WHERE STEP_EXECUTION_ID = 250101;
  DELETE FROM batch.BATCH_STEP_EXECUTION WHERE STEP_EXECUTION_ID = 250101;
  DELETE FROM batch.BATCH_JOB_EXECUTION_CONTEXT WHERE JOB_EXECUTION_ID = 250101;
  DELETE FROM batch.BATCH_JOB_EXECUTION_PARAMS WHERE JOB_EXECUTION_ID = 250101;
  DELETE FROM batch.BATCH_JOB_EXECUTION WHERE JOB_EXECUTION_ID = 250101;
  DELETE FROM batch.BATCH_JOB_INSTANCE WHERE JOB_INSTANCE_ID = 250101;

  INSERT INTO draw_result (
    id, result_slot_id, occurred_at, result_date, source_result, haiti_result, raw_payload,
    flags, status, quality, source, source_hash, fetched_at, created_at, updated_at, version
  )
  VALUES (
    v_draw_result_id, v_result_slot_id, TIMESTAMPTZ '2025-01-15 22:00:00+00', DATE '2025-01-15',
    '{"lot1":"112","lot2":"21","lot3":"25"}'::jsonb,
    '{"lot1":"112","lot2":"21","lot3":"25"}'::jsonb,
    '{"fixture":"locust-archive"}'::jsonb,
    '{}'::jsonb, 'CONFIRMED', 'OK', 'MANUAL', 'locust-archive-seed',
    TIMESTAMPTZ '2025-01-15 22:00:00+00', TIMESTAMPTZ '2025-01-15 22:00:00+00',
    TIMESTAMPTZ '2025-01-15 22:00:00+00', 0
  );

  INSERT INTO draw (
    id, tenant_id, draw_channel_id, draw_date, scheduled_at, cutoff_at, opened_at, closed_at,
    resulted_at, settled_at, status, draw_result_id, system_generated, locked, result_source,
    created_at, updated_at, version
  )
  VALUES (
    v_draw_id, v_tenant_id, v_draw_channel_id, DATE '2025-01-15',
    TIMESTAMPTZ '2025-01-15 20:00:00+00', TIMESTAMPTZ '2025-01-15 19:55:00+00',
    TIMESTAMPTZ '2025-01-15 00:00:00+00', TIMESTAMPTZ '2025-01-15 19:55:00+00',
    TIMESTAMPTZ '2025-01-15 22:00:00+00', TIMESTAMPTZ '2025-01-15 22:30:00+00',
    'SETTLED', v_draw_result_id, true, false, 'MANUAL',
    TIMESTAMPTZ '2025-01-15 00:00:00+00', TIMESTAMPTZ '2025-01-15 22:30:00+00', 0
  );

  INSERT INTO sales_ticket (
    id, tenant_id, seller_terminal_id, draw_id, draw_channel_id, ticket_code, public_code,
    verification_code, currency, stake_amount, total_amount, winning_amount, sale_status,
    sold_at, placed_at, result_status, settlement_status, sale_channel, print_status,
    print_count, created_at, updated_at, version
  )
  VALUES
    ('aaaaaaaa-1000-0000-0000-000000001001', v_tenant_id, v_terminal_id, v_draw_id, v_draw_channel_id,
     'LOC-ARCH-TCK-202501-001', 'LOCARCH001', 'LOCARCHV001', 'USD', 20.0000, 21.0000, 0.0000,
     'APPROVED', TIMESTAMPTZ '2025-01-15 15:01:00+00', TIMESTAMPTZ '2025-01-15 15:01:00+00',
     'RESULTED', 'SETTLED', 'POS', 'PRINTED', 1, TIMESTAMPTZ '2025-01-15 15:01:00+00',
     TIMESTAMPTZ '2025-01-15 15:01:00+00', 0),
    ('aaaaaaaa-1000-0000-0000-000000001002', v_tenant_id, v_terminal_id, v_draw_id, v_draw_channel_id,
     'LOC-ARCH-TCK-202501-002', 'LOCARCH002', 'LOCARCHV002', 'USD', 15.0000, 16.0000, 0.0000,
     'APPROVED', TIMESTAMPTZ '2025-01-16 16:02:00+00', TIMESTAMPTZ '2025-01-16 16:02:00+00',
     'RESULTED', 'SETTLED', 'POS', 'PRINTED', 1, TIMESTAMPTZ '2025-01-16 16:02:00+00',
     TIMESTAMPTZ '2025-01-16 16:02:00+00', 0);

  INSERT INTO sales_ticket_line (
    id, tenant_id, ticket_id, draw_id, line_number, game_code, bet_type, bet_option,
    selection_key, display_selection, stake_amount, origin, pricing_source, selection_source,
    result_status, payout_amount, created_at, updated_at, version
  )
  VALUES
    ('aaaaaaaa-1000-0000-0000-000000002001', v_tenant_id, 'aaaaaaaa-1000-0000-0000-000000001001', v_draw_id, 1, 'HT_BOLET', 'MATCH_1_2D', 1, '12', '12', 10.0000, 'CUSTOMER', 'STANDARD', 'CUSTOMER_SELECTED', 'PENDING', 0.0000, TIMESTAMPTZ '2025-01-15 15:01:00+00', TIMESTAMPTZ '2025-01-15 15:01:00+00', 0),
    ('aaaaaaaa-1000-0000-0000-000000002002', v_tenant_id, 'aaaaaaaa-1000-0000-0000-000000001001', v_draw_id, 2, 'HT_BOLET', 'MATCH_1_2D', 1, '34', '34', 10.0000, 'CUSTOMER', 'STANDARD', 'CUSTOMER_SELECTED', 'PENDING', 0.0000, TIMESTAMPTZ '2025-01-15 15:01:00+00', TIMESTAMPTZ '2025-01-15 15:01:00+00', 0),
    ('aaaaaaaa-1000-0000-0000-000000002003', v_tenant_id, 'aaaaaaaa-1000-0000-0000-000000001002', v_draw_id, 1, 'HT_BOLET', 'MATCH_1_2D', 1, '56', '56', 15.0000, 'CUSTOMER', 'STANDARD', 'CUSTOMER_SELECTED', 'PENDING', 0.0000, TIMESTAMPTZ '2025-01-16 16:02:00+00', TIMESTAMPTZ '2025-01-16 16:02:00+00', 0);

  INSERT INTO sales_ticket_charge (
    id, tenant_id, sales_ticket_id, charge_type, paid_by, amount, currency, created_at, updated_at, version
  )
  VALUES
    ('aaaaaaaa-1000-0000-0000-000000003001', v_tenant_id, 'aaaaaaaa-1000-0000-0000-000000001001', 'SMS_DELIVERY', 'BUYER', 1.0000, 'USD', TIMESTAMPTZ '2025-01-15 15:01:00+00', TIMESTAMPTZ '2025-01-15 15:01:00+00', 0),
    ('aaaaaaaa-1000-0000-0000-000000003002', v_tenant_id, 'aaaaaaaa-1000-0000-0000-000000001002', 'SMS_DELIVERY', 'BUYER', 1.0000, 'USD', TIMESTAMPTZ '2025-01-16 16:02:00+00', TIMESTAMPTZ '2025-01-16 16:02:00+00', 0);

  INSERT INTO analytics_daily (
    id, dimension_type, dimension_id, tenant_id, ref_date, tickets_sold_count, gross_sales_cents,
    stake_total_cents, created_at, updated_at
  )
  VALUES (
    'aaaaaaaa-1000-0000-0000-000000004001', 'TENANT', v_tenant_id, v_tenant_id, DATE '2025-01-15',
    2, 3700, 3500, TIMESTAMPTZ '2025-01-15 23:00:00+00', TIMESTAMPTZ '2025-01-15 23:00:00+00'
  );

  INSERT INTO analytics_draw (
    id, draw_id, tenant_id, game_code, draw_channel_code, scheduled_at, ref_date,
    tickets_sold_count, gross_sales_cents, stake_total_cents, created_at, updated_at
  )
  VALUES (
    'aaaaaaaa-1000-0000-0000-000000004002', v_draw_id, v_tenant_id, 'HT_BOLET', 'ARCHIVE',
    TIMESTAMPTZ '2025-01-15 20:00:00+00', DATE '2025-01-15',
    2, 3700, 3500, TIMESTAMPTZ '2025-01-15 23:00:00+00', TIMESTAMPTZ '2025-01-15 23:00:00+00'
  );

  INSERT INTO analytics_selection (
    id, tenant_id, ref_date, draw_channel_id, game_code, bet_type, bet_option, selection_key,
    tickets_count, stake_sum_cents, winnings_calculated_cents, created_at, updated_at
  )
  VALUES (
    'aaaaaaaa-1000-0000-0000-000000004003', v_tenant_id, DATE '2025-01-15',
    v_draw_channel_id, 'HT_BOLET', 'MATCH_1_2D', 1, '12', 1, 1000, 0,
    TIMESTAMPTZ '2025-01-15 23:00:00+00', TIMESTAMPTZ '2025-01-15 23:00:00+00'
  );

  IF v_terminal_id IS NOT NULL THEN
    INSERT INTO analytics_seller_terminal_draw (
      id, tenant_id, seller_terminal_id, draw_id, ref_date, scheduled_at, game_code,
      draw_channel_code, tickets_sold_count, gross_sales_cents, stake_total_cents,
      created_at, updated_at
    )
    VALUES (
      'aaaaaaaa-1000-0000-0000-000000004004', v_tenant_id, v_terminal_id, v_draw_id,
      DATE '2025-01-15', TIMESTAMPTZ '2025-01-15 20:00:00+00', 'HT_BOLET', 'ARCHIVE',
      2, 3700, 3500, TIMESTAMPTZ '2025-01-15 23:00:00+00', TIMESTAMPTZ '2025-01-15 23:00:00+00'
    );
  END IF;

  INSERT INTO audit_log (
    id, tenant_id, occurred_at, business_date, actor_id, actor_type, action, entity_type,
    entity_id, severity, source, correlation_id, request_id, details, created_at
  )
  VALUES (
    'aaaaaaaa-1000-0000-0000-000000005001', v_tenant_id, TIMESTAMPTZ '2025-01-15 23:10:00+00',
    DATE '2025-01-15', NULL, 'SYSTEM', 'OTHER', 'TICKET',
    'aaaaaaaa-1000-0000-0000-000000001001', 'INFO', 'MIGRATION', 'locust-archive-seed',
    'locust-archive-seed', '{"fixture":"locust-archive"}'::jsonb,
    TIMESTAMPTZ '2025-01-15 23:10:00+00'
  );

  INSERT INTO revinfo (
    rev, rev_timestamp, tenant_id, user_id, request_id, actor_type, api_scope, tenant_overridden
  )
  VALUES (
    v_rev,
    (EXTRACT(EPOCH FROM TIMESTAMPTZ '2025-01-15 23:20:00+00') * 1000)::bigint,
    v_tenant_id, NULL, 'locust-archive-seed', 'SYSTEM', 'PLATFORM', false
  );
  INSERT INTO draw_result_aud (
    id, rev, revtype, tenant_id, result_slot_id, occurred_at, result_date, source_result,
    haiti_result, flags, status, source, fetched_at, created_at, updated_at, version
  )
  VALUES (
    v_draw_result_id, v_rev, 0, v_tenant_id, v_result_slot_id,
    TIMESTAMPTZ '2025-01-15 22:00:00+00', DATE '2025-01-15',
    '{"lot1":"112"}'::jsonb, '{"lot1":"112"}'::jsonb, '{}'::jsonb, 'CONFIRMED', 'MANUAL',
    TIMESTAMPTZ '2025-01-15 22:00:00+00', TIMESTAMPTZ '2025-01-15 22:00:00+00',
    TIMESTAMPTZ '2025-01-15 22:00:00+00', 0
  );

  INSERT INTO processed_event (
    id, tenant_id, handler_key, event_id, processed_at, created_at
  )
  VALUES (
    'aaaaaaaa-1000-0000-0000-000000006001', v_tenant_id,
    'locust.archive.seed.handler', 'aaaaaaaa-1000-0000-0000-000000006101',
    TIMESTAMPTZ '2025-01-15 23:30:00+00', TIMESTAMPTZ '2025-01-15 23:30:00+00'
  );

  INSERT INTO batch.BATCH_JOB_INSTANCE (JOB_INSTANCE_ID, VERSION, JOB_NAME, JOB_KEY)
  VALUES (250101, 0, 'locustArchiveSeedJob', 'locust-archive-202501');
  INSERT INTO batch.BATCH_JOB_EXECUTION (
    JOB_EXECUTION_ID, VERSION, JOB_INSTANCE_ID, CREATE_TIME, START_TIME, END_TIME, STATUS,
    EXIT_CODE, EXIT_MESSAGE, LAST_UPDATED
  )
  VALUES (
    250101, 0, 250101, TIMESTAMP '2025-01-15 23:40:00',
    TIMESTAMP '2025-01-15 23:40:01', TIMESTAMP '2025-01-15 23:40:02',
    'COMPLETED', 'COMPLETED', 'locust archive seed', TIMESTAMP '2025-01-15 23:40:02'
  );
  INSERT INTO batch.BATCH_JOB_EXECUTION_PARAMS (
    JOB_EXECUTION_ID, PARAMETER_NAME, PARAMETER_TYPE, PARAMETER_VALUE, IDENTIFYING
  )
  VALUES (250101, 'period', 'java.lang.String', '2025-01', 'Y');
  INSERT INTO batch.BATCH_JOB_EXECUTION_CONTEXT (
    JOB_EXECUTION_ID, SHORT_CONTEXT, SERIALIZED_CONTEXT
  )
  VALUES (250101, '{}', '{}');
  INSERT INTO batch.BATCH_STEP_EXECUTION (
    STEP_EXECUTION_ID, VERSION, STEP_NAME, JOB_EXECUTION_ID, CREATE_TIME, START_TIME, END_TIME,
    STATUS, COMMIT_COUNT, READ_COUNT, FILTER_COUNT, WRITE_COUNT, READ_SKIP_COUNT, WRITE_SKIP_COUNT,
    PROCESS_SKIP_COUNT, ROLLBACK_COUNT, EXIT_CODE, EXIT_MESSAGE, LAST_UPDATED
  )
  VALUES (
    250101, 0, 'archiveSeedStep', 250101, TIMESTAMP '2025-01-15 23:40:00',
    TIMESTAMP '2025-01-15 23:40:01', TIMESTAMP '2025-01-15 23:40:02',
    'COMPLETED', 1, 1, 0, 1, 0, 0, 0, 0, 'COMPLETED', 'locust archive seed',
    TIMESTAMP '2025-01-15 23:40:02'
  );
  INSERT INTO batch.BATCH_STEP_EXECUTION_CONTEXT (
    STEP_EXECUTION_ID, SHORT_CONTEXT, SERIALIZED_CONTEXT
  )
  VALUES (250101, '{}', '{}');
END $$;

COMMIT;
