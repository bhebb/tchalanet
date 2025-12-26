package com.tchalanet.server.core.limitpolicy.application.port.out;
import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.core.limitpolicy.domain.model.LimitAssignment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LimitAssignmentReaderPort {

    List<LimitAssignment> findActiveByTarget(TenantId tenantId, String targetType, UUID targetId);

    Optional<LimitAssignment> findById(TenantId tenantId, UUID assignmentId);

    boolean existsByTenantAndLimitAndTarget(TenantId tenantId, UUID limitDefinitionId, String targetType, UUID targetId);
}
