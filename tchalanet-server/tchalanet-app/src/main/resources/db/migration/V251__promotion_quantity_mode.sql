ALTER TABLE promotion_rule_effect
    ADD COLUMN IF NOT EXISTS quantity_mode varchar(32) NOT NULL DEFAULT 'FIXED',
    ADD COLUMN IF NOT EXISTS step_paid_amount numeric(19, 4),
    ADD COLUMN IF NOT EXISTS quantity_per_step integer,
    ADD COLUMN IF NOT EXISTS max_quantity integer;

ALTER TABLE promotion_rule_effect
    ADD CONSTRAINT chk_promotion_rule_effect_quantity_mode
        CHECK (quantity_mode IN ('FIXED', 'PER_PAID_AMOUNT')),
    ADD CONSTRAINT chk_promotion_rule_effect_step_paid_amount_positive
        CHECK (step_paid_amount IS NULL OR step_paid_amount > 0),
    ADD CONSTRAINT chk_promotion_rule_effect_quantity_per_step_positive
        CHECK (quantity_per_step IS NULL OR quantity_per_step > 0),
    ADD CONSTRAINT chk_promotion_rule_effect_max_quantity_positive
        CHECK (max_quantity IS NULL OR max_quantity > 0);
