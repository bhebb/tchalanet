-- Normalize result-slot source mode so admin screens can display result acquisition without guessing.
UPDATE result_slot
   SET source_cfg = jsonb_set(source_cfg, '{source_mode}', '"EXTERNAL"'::jsonb, true),
       updated_at = now(),
       version = version + 1
 WHERE deleted_at IS NULL
   AND source_cfg->>'source_mode' IS NULL;
