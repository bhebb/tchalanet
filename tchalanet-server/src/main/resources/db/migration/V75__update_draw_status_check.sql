-- V99: Ensure draw.status CHECK allows OPEN in addition to existing values
ALTER TABLE draw DROP CONSTRAINT IF EXISTS chk_draw_status;

ALTER TABLE draw
  ADD CONSTRAINT chk_draw_status
  CHECK (status IN ('SCHEDULED','OPEN','CLOSED','RESULTED','SETTLED','CANCELED'));

