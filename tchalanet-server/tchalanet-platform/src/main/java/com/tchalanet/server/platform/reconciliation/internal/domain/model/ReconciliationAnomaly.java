package com.tchalanet.server.platform.reconciliation.internal.domain.model;

import com.tchalanet.server.common.types.id.ReconciliationAnomalyId;
import com.tchalanet.server.common.types.id.ReconciliationRunId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.id.TenantId;

import java.time.Instant;
import java.util.UUID;

/** Domain model for reconciliation anomaly. */
public record ReconciliationAnomaly(
    ReconciliationAnomalyId id,
    ReconciliationRunId runId,
    TenantId tenantId,
    String checkKey,
    String anomalyType,
    String severity,
    String status,
    String resourceType,
    UUID resourceId,
    String relatedResourceType,
    UUID relatedResourceId,
    String messageKey,
    String detailsJson,
    Instant detectedAt,
    Instant resolvedAt,
    String resolvedBy,
    String resolutionReason,
    Instant createdAt,
    UserId createdBy,
    Instant updatedAt,
    UserId updatedBy,
    Instant deletedAt,
    UserId deletedBy,
    long version
) {}

