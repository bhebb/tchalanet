-- Add occurred_at to draw_result to store the real result timestamp
ALTER TABLE draw_result
  ADD COLUMN IF NOT EXISTS occurred_at timestamptz;

-- Optionally, backfill occurred_at from updated_at or created_at if appropriate
-- UPDATE draw_result SET occurred_at = created_at WHERE occurred_at IS NULL;

-- Ensure trigger updates occurred_at is not set automatically; it's set by application on insert/update

