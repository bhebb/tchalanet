-- Add period field to draw_channel for structured period representation
-- Replaces parsing logic in DrawChannelLabelResolver.shortLabel()

ALTER TABLE draw_channel
  ADD COLUMN period varchar(32);

COMMENT ON COLUMN draw_channel.period IS 'Structured period identifier (e.g., MORNING, MIDDAY, EVENING) replacing shortLabel parsing';

-- Migrate existing data based on name patterns (optional, can be done later via admin UI)
-- This is just an example - actual migration would need to match your data
UPDATE draw_channel
SET period = CASE
  WHEN LOWER(name) LIKE '%midi%' OR LOWER(name) LIKE '%midday%' OR LOWER(name) LIKE '%noon%' THEN 'MIDDAY'
  WHEN LOWER(name) LIKE '%soir%' OR LOWER(name) LIKE '%evening%' THEN 'EVENING'
  WHEN LOWER(name) LIKE '%matin%' OR LOWER(name) LIKE '%morning%' THEN 'MORNING'
  WHEN LOWER(name) LIKE '%après-midi%' OR LOWER(name) LIKE '%afternoon%' THEN 'AFTERNOON'
  ELSE NULL
END
WHERE deleted_at IS NULL;

