-- Normalize tenant draw-channel sales windows after introducing tenant-level editing.
-- Result-slot days remain the platform default; tenants can override later from admin config.
UPDATE draw_channel dc
   SET sales_open_time = '00:00'::time,
       days_of_week = rs.days_of_week,
       updated_at = now(),
       version = dc.version + 1
  FROM result_slot rs
 WHERE dc.result_slot_id = rs.id
   AND dc.deleted_at IS NULL
   AND rs.deleted_at IS NULL
   AND (
       dc.sales_open_time IS DISTINCT FROM '00:00'::time
       OR dc.days_of_week IS DISTINCT FROM rs.days_of_week
   );
