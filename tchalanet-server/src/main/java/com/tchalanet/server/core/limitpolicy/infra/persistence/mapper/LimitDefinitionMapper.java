package com.tchalanet.server.core.limitpolicy.infra.persistence.mapper;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitAssignment;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitDefinition;
import com.tchalanet.server.core.limitpolicy.infra.persistence.entity.LimitAssignmentJpaEntity;
import com.tchalanet.server.core.limitpolicy.infra.persistence.entity.LimitDefinitionJpaEntity;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class LimitDefinitionMapper {

  public LimitDefinition toDomain(LimitDefinitionJpaEntity e) {
    return new LimitDefinition(
        e.getId(),
        TenantId.of(e.getTenantId()),
        e.getRuleKey(),
        e.isEnabled(),
        e.getOnBreach(),
        e.getParams(),
        new LimitDefinition.AppliesTo(
            (List<String>) e.getAppliesTo().get("bet_types"),
            (String) e.getAppliesTo().get("selection_pattern")),
        e.getVersion());
  }

  public LimitAssignment toAssignmentDomain(LimitAssignmentJpaEntity e) {
    return new LimitAssignment(
        e.getId(),
        TenantId.of(e.getTenantId()),
        e.getLimitDefinitionId(),
        e.getTargetType(),
        e.getTargetId(),
        e.isEnabled(),
        e.getStartsAt(),
        e.getEndsAt(),
        e.getVersion());
  }

  public LimitDefinitionJpaEntity toEntity(LimitDefinition d) {
    LimitDefinitionJpaEntity e = new LimitDefinitionJpaEntity();
    e.setId(d.id());
    e.setTenantId(d.tenantId().uuid());
    e.setRuleKey(d.ruleKey());
    e.setEnabled(d.enabled());
    e.setOnBreach(d.onBreach());
    e.setParams(d.params());
    e.setAppliesTo(
        Map.of(
            "bet_types", d.appliesTo().betTypes(),
            "selection_pattern", d.appliesTo().selectionPattern()));
    e.setVersion(d.version());
    return e;
  }

  public LimitAssignmentJpaEntity toAssignmentEntity(LimitAssignment d) {
    LimitAssignmentJpaEntity e = new LimitAssignmentJpaEntity();
    e.setId(d.id());
    e.setTenantId(d.tenantId().uuid());
    e.setLimitDefinitionId(d.limitDefinitionId());
    e.setTargetType(d.targetType());
    e.setTargetId(d.targetId());
    e.setEnabled(d.enabled());
    e.setStartsAt(d.startsAt());
    e.setEndsAt(d.endsAt());
    e.setVersion(d.version());
    return e;
  }
}
