ALTER TABLE tenant
  ADD COLUMN IF NOT EXISTS display_name varchar(255);

UPDATE tenant
SET display_name = code
WHERE display_name IS NULL OR btrim(display_name) = '';

ALTER TABLE tenant
  ALTER COLUMN display_name SET NOT NULL;
