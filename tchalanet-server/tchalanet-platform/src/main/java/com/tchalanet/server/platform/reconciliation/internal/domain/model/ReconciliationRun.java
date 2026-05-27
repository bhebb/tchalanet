package com.tchalanet.server.platform.reconciliation.internal.domain.model;

import com.tchalanet.server.common.types.id.ReconciliationRunId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.id.TenantId;

import java.time.Instant;
import java.time.LocalDate;

/** Domain model for reconciliation run. */
public record ReconciliationRun(
    ReconciliationRunId id,
    TenantId tenantId,
    String scope,
    LocalDate businessDate,
    Instant startedAt,
    Instant completedAt,
    String status,
    String triggeredBy,
    UserId triggeredByUserId,
    String reason,
    String summaryJson,
    Instant createdAt,
    UserId createdBy,
    Instant updatedAt,
    UserId updatedBy,
    Instant deletedAt,
    UserId deletedBy,
    long version
) {}

