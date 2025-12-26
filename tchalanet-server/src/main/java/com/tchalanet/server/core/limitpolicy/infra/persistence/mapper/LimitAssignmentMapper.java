package com.tchalanet.server.core.limitpolicy.infra.persistence.mapper;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitAssignment;
import com.tchalanet.server.core.limitpolicy.infra.persistence.entity.LimitAssignmentJpaEntity;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class LimitAssignmentMapper {

  public LimitAssignment toDomain(LimitAssignmentJpaEntity entity) {
    return new LimitAssignment(
        entity.getId(),
        TenantId.of(entity.getTenantId()),
        entity.getLimitDefinitionId(),
        entity.getTargetType(),
        entity.getTargetId(),
        entity.isEnabled(),
        entity.getStartsAt(),
        entity.getEndsAt(),
        entity.getVersion());
  }

  public LimitAssignmentJpaEntity toEntity(LimitAssignment domain) {
    LimitAssignmentJpaEntity entity = new LimitAssignmentJpaEntity();
    entity.setId(domain.id() != null ? domain.id() : UUID.randomUUID());
    entity.setTenantId(domain.tenantId().uuid());
    entity.setLimitDefinitionId(domain.limitDefinitionId());
    entity.setTargetType(domain.targetType());
    entity.setTargetId(domain.targetId());
    entity.setEnabled(domain.enabled());
    entity.setStartsAt(domain.startsAt());
    entity.setEndsAt(domain.endsAt());
    entity.setVersion(domain.version());
    return entity;
  }

  public void merge(LimitAssignment domain, LimitAssignmentJpaEntity entity) {
    entity.setEnabled(domain.enabled());
    entity.setStartsAt(domain.startsAt());
    entity.setEndsAt(domain.endsAt());
  }
}
