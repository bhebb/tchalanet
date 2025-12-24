package com.tchalanet.server.core.limitpolicy.application.port.out;

import com.tchalanet.server.core.limitpolicy.domain.model.LimitAssignment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LimitAssignmentReaderPort {

    List<LimitAssignment> findActiveByTarget(UUID tenantId, String targetType, UUID targetId);

    Optional<LimitAssignment> findById(UUID tenantId, UUID assignmentId);

    boolean existsByTenantAndLimitAndTarget(UUID tenantId, UUID limitDefinitionId, String targetType, UUID targetId);
}
