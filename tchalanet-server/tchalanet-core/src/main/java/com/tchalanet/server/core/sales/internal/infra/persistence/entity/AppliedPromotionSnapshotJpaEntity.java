package com.tchalanet.server.core.sales.internal.infra.persistence.entity;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
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
    name = "applied_promotion_snapshot",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_applied_promotion_tenant_ticket_decision",
        columnNames = {"tenant_id", "ticket_id", "promotion_decision_id"}
    )
)
@Getter
@Setter
public class AppliedPromotionSnapshotJpaEntity extends BaseTenantEntity {
    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Column(name = "promotion_decision_id", nullable = false)
    private UUID promotionDecisionId;

    @Column(name = "decision_status", nullable = false, length = 32)
    private String decisionStatus;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "snapshot_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> snapshotJson;
}
