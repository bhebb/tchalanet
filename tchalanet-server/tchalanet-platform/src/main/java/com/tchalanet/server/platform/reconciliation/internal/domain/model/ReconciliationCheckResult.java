package com.tchalanet.server.platform.reconciliation.internal.domain.model;

import com.tchalanet.server.common.types.id.ReconciliationCheckResultId;
import com.tchalanet.server.common.types.id.ReconciliationRunId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.id.TenantId;

import java.time.Instant;

/** Domain model for reconciliation check result. */
public record ReconciliationCheckResult(
    ReconciliationCheckResultId id,
    ReconciliationRunId runId,
    TenantId tenantId,
    String checkKey,
    boolean passed,
    String detailsJson,
    Instant checkedAt,
    Instant createdAt,
    UserId createdBy,
    Instant updatedAt,
    UserId updatedBy,
    Instant deletedAt,
    UserId deletedBy,
    long version
) {}

