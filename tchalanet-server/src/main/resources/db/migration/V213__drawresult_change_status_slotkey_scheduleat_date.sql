ALTER TABLE draw_result
DROP
CONSTRAINT IF EXISTS chk_draw_result__status;

ALTER TABLE draw_result
    ADD CONSTRAINT chk_draw_result__status
        CHECK (status IN ('PROVISIONAL', 'CONFIRMED', 'OVERRIDDEN', 'ERROR'));

CREATE INDEX IF NOT EXISTS ix_draw_result__slot_occurred
    ON draw_result (result_slot_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS ix_draw_result__source_hash
    ON draw_result (source_hash);
