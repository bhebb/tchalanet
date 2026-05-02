ALTER TABLE draw_result
    ADD CONSTRAINT uq_draw_result__slot_occurred
        UNIQUE (result_slot_id, occurred_at);
