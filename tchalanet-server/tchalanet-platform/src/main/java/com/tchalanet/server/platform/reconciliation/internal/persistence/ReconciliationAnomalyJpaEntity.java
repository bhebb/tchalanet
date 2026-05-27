package com.tchalanet.server.platform.reconciliation.internal.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
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
 * JPA entity for reconciliation anomaly
 * Table: reconciliation_anomaly
 */
@Entity
@Table(name = "reconciliation_anomaly")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationAnomalyJpaEntity extends BaseTenantEntity {

    @Column(name = "run_id", columnDefinition = "uuid", nullable = false)
    private UUID runId;

    @Column(name = "check_key", length = 256, nullable = false)
    private String checkKey;

    @Column(name = "anomaly_type", length = 128, nullable = false)
    private String anomalyType;

    @Column(name = "severity", length = 32)
    private String severity;

    @Column(name = "status", length = 32)
    private String status;

    @Column(name = "resource_type", length = 128)
    private String resourceType;

    @Column(name = "resource_id", columnDefinition = "uuid")
    private UUID resourceId;

    @Column(name = "related_resource_type", length = 128)
    private String relatedResourceType;

    @Column(name = "related_resource_id", columnDefinition = "uuid")
    private UUID relatedResourceId;

    @Column(name = "message_key", length = 256)
    private String messageKey;

    @Column(name = "details_json", columnDefinition = "jsonb")
    private String detailsJson;

    @Column(name = "detected_at")
    private Instant detectedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved_by", length = 128)
    private String resolvedBy;

    @Column(name = "resolution_reason", length = 512)
    private String resolutionReason;
}


