package com.tchalanet.server.core.sales.internal.infra.persistence.entity.preparation;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
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

    @Column(name = "payout_base_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal payoutBaseAmount;

    @Column(name = "promotion_decision_id")
    private UUID promotionDecisionId;

    @Column(name = "promotion_rule_id")
    private UUID promotionRuleId;

    @Column(name = "regenerable", nullable = false)
    private boolean regenerable;

    @Column(name = "max_regenerations", nullable = false)
    private int maxRegenerations = 3;

    @Column(name = "regeneration_count", nullable = false)
    private int regenerationCount;
}
