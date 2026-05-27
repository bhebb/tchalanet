package com.tchalanet.server.platform.reconciliation.internal.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for reconciliation check result
 * Table: reconciliation_check_result
 */
@Entity
@Table(name = "reconciliation_check_result")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationCheckResultJpaEntity extends BaseEntity {

    @Column(name = "run_id", columnDefinition = "uuid", nullable = false)
    private UUID runId;

    @Column(name = "tenant_id", columnDefinition = "uuid")
    private UUID tenantId;

    @Column(name = "check_key", length = 256, nullable = false)
    private String checkKey;

    @Column(name = "status", length = 32)
    private String status;

    @Column(name = "severity", length = 32)
    private String severity;

    @Column(name = "expected_count")
    private Long expectedCount;

    @Column(name = "actual_count")
    private Long actualCount;

    @Column(name = "anomaly_count")
    private Long anomalyCount;

    @Column(name = "summary_json", columnDefinition = "jsonb")
    private String summaryJson;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}

