package com.tchalanet.server.core.sales.internal.infra.persistence.entity.preparation;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.core.promotion.api.model.PromotionChoiceMode;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineSelectionSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "sale_preparation_promotion_line")
@Getter
@Setter
public class SalePreparationPromotionLineJpaEntity extends BaseTenantEntity {

    @Column(name = "preparation_id", nullable = false)
    private UUID preparationId;

    @Column(name = "line_ref", nullable = false, length = 36)
    private String lineRef;

    @Column(name = "game_code", nullable = false, length = 64)
    private String gameCode;

    @Column(name = "bet_type", nullable = false, length = 32)
    private String betType;

    @Column(name = "bet_option")
    private Short betOption;

    @Column(name = "selection", nullable = false, length = 32)
    private String selection;

    @Enumerated(EnumType.STRING)
    @Column(name = "selection_source", nullable = false, length = 32)
    private TicketLineSelectionSource selectionSource = TicketLineSelectionSource.PROMOTION_GENERATED;

    @Enumerated(EnumType.STRING)
    @Column(name = "choice_mode", length = 32)
    private PromotionChoiceMode choiceMode;

    @Column(name = "promotion_decision_id")
    private UUID promotionDecisionId;

    @Column(name = "promotion_rule_id")
    private UUID promotionRuleId;

    @Column(name = "promotion_rule_key", length = 96)
    private String promotionRuleKey;

    @Column(name = "promotion_effect_type", length = 32)
    private String promotionEffectType;

    @Column(name = "promotion_decision_context_hash", length = 128)
    private String promotionDecisionContextHash;

    @Column(name = "promotion_decision_engine_version", length = 48)
    private String promotionDecisionEngineVersion;

    @Column(name = "regenerable", nullable = false)
    private boolean regenerable;

    @Column(name = "max_regenerations", nullable = false)
    private int maxRegenerations = 3;

    @Column(name = "regeneration_count", nullable = false)
    private int regenerationCount;
}
