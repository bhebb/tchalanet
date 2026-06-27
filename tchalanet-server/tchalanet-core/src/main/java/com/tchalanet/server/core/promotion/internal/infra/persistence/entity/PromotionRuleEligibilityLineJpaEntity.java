package com.tchalanet.server.core.promotion.internal.infra.persistence.entity;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "promotion_rule_eligibility_line")
@Getter
@Setter
public class PromotionRuleEligibilityLineJpaEntity extends BaseTenantEntity {
    @Column(name = "rule_id", nullable = false)
    private UUID ruleId;

    @Column(name = "game_code", nullable = false, length = 64)
    private String gameCode;

    @Column(name = "min_count", nullable = false)
    private int minCount;
}
