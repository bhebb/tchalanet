-- maryaj-gratis-auto-selection-v1 — slice 3 (Promotion model)
-- Selection auto-generation fields on promotion_rule_effect.
-- choice_mode persists PromotionChoiceMode (was not persisted before).
-- LOW_EXPOSURE_RANDOM is accepted by the schema but rejected by the
-- application everywhere in V1 (effect validation, activation, generation).

ALTER TABLE promotion_rule_effect
  ADD COLUMN choice_mode varchar(32) NULL,
  ADD COLUMN generation_strategy varchar(32) NULL,
  ADD COLUMN regenerable_before_confirm boolean NOT NULL DEFAULT false,
  ADD COLUMN max_regenerations_before_confirm integer NOT NULL DEFAULT 3;

ALTER TABLE promotion_rule_effect
  ADD CONSTRAINT chk_promotion_rule_effect_choice_mode
    CHECK (choice_mode IS NULL OR choice_mode IN ('NONE','CUSTOMER_SELECTS','SELLER_SELECTS','AUTO_GENERATE')),
  ADD CONSTRAINT chk_promotion_rule_effect_generation_strategy
    CHECK (generation_strategy IS NULL OR generation_strategy IN ('RANDOM','LOW_EXPOSURE_RANDOM')),
  ADD CONSTRAINT chk_promotion_rule_effect_max_regen
    CHECK (max_regenerations_before_confirm >= 0);

ALTER TABLE promotion_rule_effect_aud
  ADD COLUMN choice_mode varchar(32) NULL,
  ADD COLUMN generation_strategy varchar(32) NULL,
  ADD COLUMN regenerable_before_confirm boolean NULL,
  ADD COLUMN max_regenerations_before_confirm integer NULL;
