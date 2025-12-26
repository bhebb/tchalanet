package com.tchalanet.server.core.limitpolicy.application.port.out;

import com.tchalanet.server.common.types.enums.TargetType;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitAssignment;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitDefinition;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LimitDefinitionReaderPort {
  List<LimitDefinition> findActiveByTenantId(TenantId tenantId);

  List<LimitAssignment> findActiveAssignmentsByTenantId(TenantId tenantId);

  List<LimitAssignment> findActiveAssignmentsByTenantAndTarget(
      TenantId tenantId, TargetType targetType, UUID targetId);

  Optional<LimitDefinition> findById(UUID definitionId);
}
