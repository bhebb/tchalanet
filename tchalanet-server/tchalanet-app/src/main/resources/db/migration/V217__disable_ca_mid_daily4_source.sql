UPDATE result_slot
   SET source_cfg = jsonb_set(source_cfg, '{pick4,active}', 'false'::jsonb, true),
       updated_at = now(),
       version = version + 1
 WHERE slot_key = 'CA_MID'
   AND provider = 'CA'
   AND source_cfg #>> '{pick4,game_code}' = 'DAILY4'
   AND COALESCE((source_cfg #>> '{pick4,active}')::boolean, true) = true;
