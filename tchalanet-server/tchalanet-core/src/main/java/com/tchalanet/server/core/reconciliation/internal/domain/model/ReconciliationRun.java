package com.tchalanet.server.core.reconciliation.internal.domain.model;

import com.tchalanet.server.common.types.id.ReconciliationRunId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public record ReconciliationRun(
    ReconciliationRunId id,
    TenantId tenantId,
    LocalDate businessDate,
    ReconciliationRunType runType,
    ReconciliationRunStatus status,
    boolean forced,
    String reason,
    Instant startedAt,
    Instant completedAt,
    long checkedDrawCount,
    long checkedTicketCount,
    long anomalyCount,
    long criticalCount,
    long highCount,
    long mediumCount,
    long lowCount
) {
    public ReconciliationRun {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(tenantId, "tenantId is required");
        Objects.requireNonNull(businessDate, "businessDate is required");
        Objects.requireNonNull(runType, "runType is required");
        Objects.requireNonNull(status, "status is required");
        Objects.requireNonNull(startedAt, "startedAt is required");
        if (completedAt != null && completedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("reconciliation.completed_before_started");
        }
    }
}
