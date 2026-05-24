package com.tchalanet.server.core.promotion.internal.infra.persistence.entity;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

@Entity
@Audited
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

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "evaluation_phase", nullable = false, length = 48)
    private String evaluationPhase;

    @Column(nullable = false)
    private int priority;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "eligibility_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> eligibilityJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "effects_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> effectsJson;

    @Column(name = "quota_key", length = 96)
    private String quotaKey;

    @Column(name = "max_uses")
    private Integer maxUses;
}
