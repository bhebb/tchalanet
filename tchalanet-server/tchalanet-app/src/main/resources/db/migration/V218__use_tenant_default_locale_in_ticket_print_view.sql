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
       dc.code                      AS draw_channel_code,
       dc.name                      AS draw_channel_name,
       COALESCE(dc.name, dc.code)    AS draw_channel_display_name,
       rs.slot_key                  AS result_slot_key,
       rs.provider                  AS result_provider,
       COALESCE(rs.timezone, dc.timezone) AS result_timezone,

       NULL::uuid                   AS outlet_id,
       NULL::varchar                AS outlet_code,
       NULL::varchar                AS outlet_name,
       NULL::text                   AS outlet_receipt_header,
       NULL::text                   AS outlet_receipt_footer,

       t.seller_terminal_id         AS terminal_id,
       st.terminal_code             AS terminal_code,
       st.display_name              AS terminal_label,

       NULL::uuid                   AS sales_session_id,
       NULL::text                   AS session_code,

       NULL::uuid                   AS seller_user_id,
       COALESCE(st.display_name, t.seller_terminal_id::text) AS seller_display_name,

       COALESCE(
           NULLIF(tn.display_name, ''),
           tn.code,
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
       COALESCE(NULLIF(tn.default_locale, ''), NULLIF(tn.default_language, ''), 'fr') AS locale,
       COALESCE(NULLIF(tn.timezone, ''), 'America/Port-au-Prince') AS timezone,

       t.seller_terminal_id         AS seller_terminal_id,
       st.terminal_code             AS seller_terminal_code,
       st.display_name              AS seller_terminal_label
FROM sales_ticket t
JOIN draw d
    ON d.id = t.draw_id
LEFT JOIN draw_channel dc
    ON dc.id = t.draw_channel_id
LEFT JOIN result_slot rs
    ON rs.id = dc.result_slot_id
LEFT JOIN seller_terminal st
    ON st.id = t.seller_terminal_id
LEFT JOIN tenant tn
    ON tn.id = t.tenant_id
WHERE t.deleted_at IS NULL;
