ALTER TABLE promotion_rule_effect
    ADD COLUMN IF NOT EXISTS quantity_tiers jsonb NOT NULL DEFAULT '[]'::jsonb;

ALTER TABLE promotion_rule_effect
    DROP CONSTRAINT IF EXISTS chk_promotion_rule_effect_quantity_mode;

ALTER TABLE promotion_rule_effect
    ADD CONSTRAINT chk_promotion_rule_effect_quantity_mode
        CHECK (quantity_mode IN ('FIXED', 'PER_PAID_AMOUNT', 'TIERED_PAID_AMOUNT')),
    ADD CONSTRAINT chk_promotion_rule_effect_quantity_tiers_array
        CHECK (jsonb_typeof(quantity_tiers) = 'array');
