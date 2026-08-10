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
       dr.haiti_result   AS haiti_result,
       dr.fetched_at     AS draw_result_fetched_at

FROM draw d
         JOIN draw_channel dc ON dc.id = d.draw_channel_id
         JOIN result_slot rs ON rs.id = dc.result_slot_id
         LEFT JOIN draw_result dr ON dr.id = d.draw_result_id
WHERE d.deleted_at IS NULL
  AND dc.deleted_at IS NULL;
