package com.tchalanet.server.core.limitpolicy.internal.infra.persistence.assignment.mapper;

import com.tchalanet.server.common.types.id.LimitAssignmentId;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitAssignment;
import com.tchalanet.server.core.limitpolicy.infra.persistence.assignment.LimitAssignmentJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LimitAssignmentMapper {

    private final LimitScopeMapper scopeMapper;

    public LimitAssignment toDomain(LimitAssignmentJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return new LimitAssignment(
            LimitAssignmentId.of(entity.getId()),
            entity.getRuleKey(),
            scopeMapper.toDomain(entity.getScopeType(), entity.getScopeId()),
            entity.isEnabled(),
            entity.getOnBreach(),
            entity.getParams(),
            entity.getStartsAt(),
            entity.getEndsAt(),
            entity.getDeletedAt() != null
        );
    }

    public LimitAssignmentJpaEntity toEntity(LimitAssignment assignment) {
        if (assignment == null) {
            return null;
        }

        var entity = new LimitAssignmentJpaEntity();

        if (assignment.id() != null) {
            entity.setId(assignment.id().value());
        }

        entity.setRuleKey(assignment.ruleKey());
        entity.setScopeType(scopeMapper.toType(assignment.scope()));
        entity.setScopeId(scopeMapper.toId(assignment.scope()));
        entity.setEnabled(assignment.enabled());
        entity.setOnBreach(assignment.onBreach());
        entity.setParams(assignment.params());
        entity.setStartsAt(assignment.startsAt());
        entity.setEndsAt(assignment.endsAt());

        return entity;
    }
}
