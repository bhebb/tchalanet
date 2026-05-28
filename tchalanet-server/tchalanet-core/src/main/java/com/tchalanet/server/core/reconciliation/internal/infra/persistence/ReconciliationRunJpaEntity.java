package com.tchalanet.server.core.reconciliation.internal.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.core.reconciliation.internal.domain.model.ReconciliationRunStatus;
import com.tchalanet.server.core.reconciliation.internal.domain.model.ReconciliationRunType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "reconciliation_run",
    indexes = {
        @Index(name = "idx_reconciliation_run_tenant_date", columnList = "tenant_id,business_date")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class ReconciliationRunJpaEntity extends BaseTenantEntity {

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "run_type", nullable = false, length = 32)
    private ReconciliationRunType runType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ReconciliationRunStatus status;

    @Column(name = "forced", nullable = false)
    private boolean forced;

    @Column(name = "reason", columnDefinition = "text")
    private String reason;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "checked_draw_count", nullable = false)
    private long checkedDrawCount;

    @Column(name = "checked_ticket_count", nullable = false)
    private long checkedTicketCount;

    @Column(name = "anomaly_count", nullable = false)
    private long anomalyCount;

    @Column(name = "critical_count", nullable = false)
    private long criticalCount;

    @Column(name = "high_count", nullable = false)
    private long highCount;

    @Column(name = "medium_count", nullable = false)
    private long mediumCount;

    @Column(name = "low_count", nullable = false)
    private long lowCount;
}
