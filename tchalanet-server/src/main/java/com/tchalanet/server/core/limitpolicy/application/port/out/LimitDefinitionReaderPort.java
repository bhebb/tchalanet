package com.tchalanet.server.core.limitpolicy.application.port.out;

import com.tchalanet.server.core.limitpolicy.domain.model.LimitAssignment;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitDefinition;
import com.tchalanet.server.core.limitpolicy.domain.model.TargetType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LimitDefinitionReaderPort {
  List<LimitDefinition> findActiveByTenantId(UUID tenantId);
  List<LimitAssignment> findActiveAssignmentsByTenantId(UUID tenantId);
  List<LimitAssignment> findActiveAssignmentsByTenantAndTarget(UUID tenantId, TargetType targetType, UUID targetId);
  Optional<LimitDefinition> findById(UUID definitionId);
}
