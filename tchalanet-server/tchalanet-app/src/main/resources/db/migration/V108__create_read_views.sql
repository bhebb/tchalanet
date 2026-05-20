-- Read-model views for tickets and draws.
-- These views are SELECT only — never for writes, settlements, or domain transitions.
-- SECURITY INVOKER: RLS from underlying tables applies to the querying role.

-- ─── sales_ticket_print_header_v ──────────────────────────────────────────────
-- Usage: print header read-model for ticket printing.
CREATE OR REPLACE VIEW sales_ticket_print_header_v
  WITH (security_invoker = true)
AS
SELECT t.id                         AS ticket_id,
       t.tenant_id,

       t.ticket_code,
       t.public_code,
       t.verification_code,

       t.sale_status,
       t.result_status,
       t.settlement_status,

       COALESCE(t.print_status, 'NOT_PRINTED') AS print_status,
       COALESCE(t.print_count, 0) AS print_count,
       t.first_printed_at,
       t.last_printed_at,

       t.draw_id,
       d.draw_date,
       d.scheduled_at,
       d.cutoff_at,

       dc.id                        AS draw_channel_id,
       dc.name                      AS draw_channel_name,
       COALESCE(dc.name, dc.code)    AS draw_channel_display_name,

       o.id                         AS outlet_id,
       o.slug                       AS outlet_code,
       o.name                       AS outlet_name,
       o.receipt_header_message     AS outlet_receipt_header,
       o.receipt_footer_message     AS outlet_receipt_footer,

       tm.id                        AS terminal_id,
       COALESCE(tm.inventory_tag, tm.label, tm.id::text) AS terminal_code,
       tm.label                     AS terminal_label,

       ss.id                        AS sales_session_id,
       ss.id::text                  AS session_code,

       t.seller_user_id,
       COALESCE(au.display_name, au.email::text) AS seller_display_name,

       COALESCE(tn.name, o.name, 'Tchalanet') AS tenant_display_name,
       NULLIF(
           COALESCE(
               tn.config #>> '{receipt,header}',
               ''
           ),
           ''
       )                            AS tenant_receipt_header,
       NULLIF(
           COALESCE(
               tn.config #>> '{receipt,footer}',
               ''
           ),
           ''
       )                            AS tenant_receipt_footer,

       t.stake_amount,
       t.total_amount,
       t.potential_payout_amount,
       t.currency,

       t.placed_at,
       t.sale_channel               AS sale_origin,
       COALESCE(up.locale, 'fr')     AS locale,
       COALESCE(up.time_zone, o.timezone, 'America/Port-au-Prince') AS timezone
FROM sales_ticket t
JOIN draw d
    ON d.id = t.draw_id
LEFT JOIN draw_channel dc
    ON dc.id = t.draw_channel_id
LEFT JOIN outlet o
    ON o.id = t.outlet_id
LEFT JOIN tenant tn
    ON tn.id = t.tenant_id
LEFT JOIN terminal tm
    ON tm.id = t.terminal_id
LEFT JOIN sales_session ss
    ON ss.id = t.sales_session_id
LEFT JOIN app_user au
    ON au.id = t.seller_user_id
LEFT JOIN user_preference up
    ON up.user_id = au.id
WHERE t.deleted_at IS NULL;


-- ─── v_draw_summary ──────────────────────────────────────────────────────────
-- Usage: public draws, next draws, latest results, dashboard, ticket labels.
CREATE OR REPLACE VIEW v_draw_summary
  WITH (security_invoker = true)
AS
SELECT d.id              AS draw_id,
       d.tenant_id,
       d.draw_date,
       d.status,
       d.scheduled_at,
       d.opened_at,
       d.closed_at,
       d.cutoff_at,
       d.resulted_at,
       d.settled_at,

       dc.id             AS draw_channel_id,
       dc.code           AS draw_channel_code,
       dc.name           AS draw_channel_label,
       dc.period         AS draw_channel_period,
       dc.draw_time,
       dc.timezone       AS draw_timezone,
       dc.active         AS draw_channel_active,

       rs.id             AS result_slot_id,
       rs.slot_key       AS result_slot_key,
       rs.provider       AS result_provider,
       rs.timezone       AS result_timezone,
       rs.draw_time      AS result_draw_time,
       rs.active         AS result_active,

       dr.id             AS draw_result_id,
       dr.status         AS draw_result_status,
       dr.occurred_at    AS draw_result_occurred_at,
       dr.source_hash,
       dr.haiti_result   AS haiti_result

FROM draw d
         JOIN draw_channel dc ON dc.id = d.draw_channel_id
         JOIN result_slot rs ON rs.id = dc.result_slot_id
         LEFT JOIN draw_result dr ON dr.id = d.draw_result_id
WHERE d.deleted_at IS NULL
  AND dc.deleted_at IS NULL;
