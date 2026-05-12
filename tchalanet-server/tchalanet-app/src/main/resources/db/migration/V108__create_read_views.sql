-- Read-model views for tickets and draws.
-- These views are SELECT only — never for writes, settlements, or domain transitions.
-- SECURITY INVOKER: RLS from underlying tables applies to the querying role.
--
-- IMPORTANT: any change to ticket / terminal / outlet / draw / draw_channel /
-- result_slot / draw_result / sales_session / address must verify these views.

-- ─── v_ticket_summary ────────────────────────────────────────────────────────
-- Usage: ticket lists, dashboards, tenant/admin views.
-- Lines are NOT included — query ticket_line separately when needed.
CREATE OR REPLACE VIEW v_ticket_summary
  WITH (security_invoker = true)
AS
SELECT t.id                  AS ticket_id,
       t.tenant_id,
       t.ticket_code,
       t.public_code,
       t.sale_status,
       t.result_status,
       t.settlement_status,
       t.currency,
       t.total_amount_cents,
       t.winning_amount_cents,
       t.created_at,
       t.updated_at,
       t.user_id              AS seller_user_id,
       t.session_id,
       tm.id                  AS terminal_id,
       tm.label               AS terminal_label,
       o.id                   AS outlet_id,
       o.name                 AS outlet_name,
       d.id                   AS draw_id,
       d.draw_date,
       d.scheduled_at,
       dc.id                  AS draw_channel_id,
       dc.code                AS draw_channel_code,
       dc.name                AS draw_channel_label,
       dc.period              AS draw_channel_period,
       dc.timezone            AS draw_timezone
FROM ticket t
         JOIN outlet o ON o.id = t.outlet_id
         LEFT JOIN terminal tm ON tm.id = t.terminal_id
         JOIN draw d ON d.id = t.draw_id
         JOIN draw_channel dc ON dc.id = d.draw_channel_id
WHERE t.deleted_at IS NULL;


-- ─── v_ticket_print ──────────────────────────────────────────────────────────
-- Usage: PDF/ESC-POS receipt printing. Header only — query ticket_line separately.
CREATE OR REPLACE VIEW v_ticket_print
  WITH (security_invoker = true)
AS
SELECT t.id                   AS ticket_id,
       t.tenant_id,
       t.ticket_code,
       t.public_code,
       t.sale_status,
       t.result_status,
       t.settlement_status,
       t.currency,
       t.total_amount_cents,
       t.winning_amount_cents,
       t.created_at           AS sold_at,
       t.user_id              AS seller_user_id,
       tm.id                  AS terminal_id,
       tm.label               AS terminal_label,
       ss.id                  AS session_id,
       o.id                   AS outlet_id,
       o.name                 AS outlet_name,
       a.city                 AS outlet_city,
       a.country              AS outlet_country,
       d.id                   AS draw_id,
       d.draw_date,
       d.scheduled_at,
       dc.id                  AS draw_channel_id,
       dc.code                AS draw_channel_code,
       dc.name                AS draw_channel_label,
       dc.period              AS draw_channel_period,
       dc.draw_time           AS draw_time,
       dc.timezone            AS draw_timezone
FROM ticket t
         JOIN outlet o ON o.id = t.outlet_id
         LEFT JOIN terminal tm ON tm.id = t.terminal_id
         LEFT JOIN address a ON a.id = o.address_id
         JOIN sales_session ss ON ss.id = t.session_id
         JOIN draw d ON d.id = t.draw_id
         JOIN draw_channel dc ON dc.id = d.draw_channel_id
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
