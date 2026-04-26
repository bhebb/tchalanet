-- Migrate legacy DrawResultStatus values to new standard (D2)
-- Mapping: VALID -> FINAL, OVERRIDDEN -> FINAL, INVALIDATED -> ERROR

UPDATE draw_result SET status = 'FINAL' WHERE status IN ('VALID', 'OVERRIDDEN');
UPDATE draw_result SET status = 'ERROR' WHERE status = 'INVALIDATED';

-- Ensure all existing rows have valid status
UPDATE draw_result SET status = 'PROVISIONAL' WHERE status NOT IN ('PROVISIONAL', 'FINAL', 'ERROR');
