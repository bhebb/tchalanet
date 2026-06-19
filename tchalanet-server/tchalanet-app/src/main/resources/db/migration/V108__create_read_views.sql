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

       NULL::uuid                   AS outlet_id,
       NULL::varchar                AS outlet_code,
       NULL::varchar                AS outlet_name,
       NULL::text                   AS outlet_receipt_header,
       NULL::text                   AS outlet_receipt_footer,

       t.seller_terminal_id         AS terminal_id,
       NULL::varchar                AS terminal_code,
       NULL::varchar                AS terminal_label,

       NULL::uuid                   AS sales_session_id,
       NULL::text                   AS session_code,

       NULL::uuid                   AS seller_user_id,
       t.seller_terminal_id::text   AS seller_display_name,

       COALESCE(
           NULLIF(tn.config #>> '{document,receipt,displayName}', ''),
           tn.name,
           'Tchalanet'
       )                            AS tenant_display_name,
       NULLIF(
           COALESCE(
               tn.config #>> '{document,receipt,headerMessage}',
               ''
           ),
           ''
       )                            AS tenant_receipt_header,
       NULLIF(
           COALESCE(
               tn.config #>> '{document,receipt,footerMessage}',
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
       'fr'                         AS locale,
       COALESCE(NULLIF(tn.config #>> '{timezone}', ''), 'America/Port-au-Prince') AS timezone
FROM sales_ticket t
JOIN draw d
    ON d.id = t.draw_id
LEFT JOIN draw_channel dc
    ON dc.id = t.draw_channel_id
LEFT JOIN tenant tn
    ON tn.id = t.tenant_id
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
