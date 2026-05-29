package com.tchalanet.server.core.promotion.internal.infra.persistence.entity;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Entity
@Audited
@Table(name = "promotion_decision", uniqueConstraints = @UniqueConstraint(name = "uq_promotion_decision_tenant_hash_phase", columnNames = {"tenant_id", "context_hash", "evaluation_phase"}))
@Getter
@Setter
public class PromotionDecisionJpaEntity extends BaseTenantEntity {
    @Column(name = "decision_status", nullable = false, length = 32)
    private String decisionStatus;

    @Column(name = "evaluation_phase", nullable = false, length = 48)
    private String evaluationPhase;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;

    @Column(name = "context_hash", nullable = false, length = 128)
    private String contextHash;

    @Column(name = "engine_version", nullable = false, length = 48)
    private String engineVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "decision_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> decisionJson;
}
