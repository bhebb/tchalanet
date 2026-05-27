package com.tchalanet.server.platform.reconciliation.internal.domain.model;

import com.tchalanet.server.common.types.id.ReconciliationRepairActionId;
import com.tchalanet.server.common.types.id.ReconciliationAnomalyId;
import com.tchalanet.server.common.types.id.ReconciliationRunId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.id.TenantId;

import java.time.Instant;

/** Domain model for reconciliation repair action. */
public record ReconciliationRepairAction(
    ReconciliationRepairActionId id,
    ReconciliationAnomalyId anomalyId,
    ReconciliationRunId runId,
    TenantId tenantId,
    String actionType,
    String status,
    String commandName,
    String commandPayloadJson,
    Instant executedAt,
    String executedBy,
    String failureMessage,
    Instant createdAt,
    UserId createdBy,
    Instant updatedAt,
    UserId updatedBy,
    Instant deletedAt,
    UserId deletedBy,
    long version
) {}

