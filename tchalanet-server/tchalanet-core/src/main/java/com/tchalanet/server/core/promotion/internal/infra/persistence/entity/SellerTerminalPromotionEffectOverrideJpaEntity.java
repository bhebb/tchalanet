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
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "seller_terminal_promotion_effect_override")
@Getter
@Setter
public class SellerTerminalPromotionEffectOverrideJpaEntity extends BaseTenantEntity {
    @Column(name = "seller_terminal_id", nullable = false)
    private UUID sellerTerminalId;

    @Column(name = "campaign_id", nullable = false)
    private UUID campaignId;

    @Column(name = "rule_id", nullable = false)
    private UUID ruleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "effect_type", nullable = false, length = 32)
    private PromotionEffectType effectType;

    @Column(name = "game_code", nullable = false, length = 64)
    private String gameCode = "*";

    @Column(name = "effect_enabled")
    private Boolean effectEnabled;

    @Column(name = "quantity")
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "choice_mode", length = 32)
    private PromotionChoiceMode choiceMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "generation_strategy", length = 32)
    private SelectionGenerationStrategy generationStrategy;

    @Column(name = "regenerable_before_confirm")
    private Boolean regenerableBeforeConfirm;

    @Column(name = "max_regenerations_before_confirm")
    private Integer maxRegenerationsBeforeConfirm;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
