package com.tchalanet.server.core.promotion.internal.infra.persistence.entity;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "promotion_rule",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_promotion_rule_tenant_campaign_key",
        columnNames = {"tenant_id", "campaign_id", "rule_key"}
    )
)
@Getter
@Setter
public class PromotionRuleJpaEntity extends BaseTenantEntity {

    @Column(name = "campaign_id", nullable = false)
    private UUID campaignId;

    @Column(name = "rule_key", nullable = false, length = 96)
    private String ruleKey;

    @Column(nullable = false)
    private int priority;

    @Column(name = "min_paid_total", precision = 19, scale = 4)
    private BigDecimal minPaidTotal;

    @Column(name = "before_local_time")
    private LocalTime beforeLocalTime;

}
