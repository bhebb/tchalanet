-- Index for performance when checking settled draws for a result or finding draws by result
CREATE INDEX IF NOT EXISTS ix_draw_draw_result_id
    ON draw (draw_result_id)
    WHERE deleted_at IS NULL;
