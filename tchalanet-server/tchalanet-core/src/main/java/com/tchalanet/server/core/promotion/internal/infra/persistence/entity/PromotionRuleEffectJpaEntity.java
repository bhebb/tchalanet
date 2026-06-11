package com.tchalanet.server.core.promotion.internal.infra.persistence.entity;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.core.promotion.api.model.PromotionChoiceMode;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffectType;
import com.tchalanet.server.core.selection.api.model.SelectionGenerationStrategy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Audited
@Table(name = "promotion_rule_effect")
@Getter
@Setter
public class PromotionRuleEffectJpaEntity extends BaseTenantEntity {
    @Column(name = "rule_id", nullable = false)
    private UUID ruleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "effect_type", nullable = false, length = 32)
    private PromotionEffectType effectType;

    @Column(name = "game_code", length = 64)
    private String gameCode;

    @Column(name = "payout_base_amount", precision = 19, scale = 4)
    private BigDecimal payoutBaseAmount;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "odds_override", precision = 19, scale = 6)
    private BigDecimal oddsOverride;

    @Column(name = "charge_type", length = 64)
    private String chargeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "choice_mode", length = 32)
    private PromotionChoiceMode choiceMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "generation_strategy", length = 32)
    private SelectionGenerationStrategy generationStrategy;

    @Column(name = "regenerable_before_confirm", nullable = false)
    private boolean regenerableBeforeConfirm;

    @Column(name = "max_regenerations_before_confirm", nullable = false)
    private int maxRegenerationsBeforeConfirm = 3;
}
