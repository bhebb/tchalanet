package com.tchalanet.server.core.limitpolicy.infra.persistence.mapper;

import com.tchalanet.server.common.types.id.LimitAssignmentId;
import com.tchalanet.server.common.types.id.LimitDefinitionId;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitAssignment;
import com.tchalanet.server.core.limitpolicy.infra.persistence.entity.LimitAssignmentJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LimitAssignmentMapper {

    private final LimitTargetMapper targetMapper;

    public LimitAssignment toDomain(LimitAssignmentJpaEntity e) {
        return new LimitAssignment(
            LimitAssignmentId.of(e.getId()),
            LimitDefinitionId.of(e.getLimitDefinitionId()),
            targetMapper.toDomain(e.getTargetType(), e.getTargetId()),
            e.isEnabled(),
            e.getStartsAt(),
            e.getEndsAt(),
            e.getParamsOverride(),
            e.getAppliesToOverride(),
            e.getDeletedAt()
        );
    }

    public LimitAssignmentJpaEntity toEntity(LimitAssignment d) {
        var e = new LimitAssignmentJpaEntity();
        e.setId(d.id().value());
        e.setLimitDefinitionId(d.limitDefinitionId().value());
        e.setTargetType(targetMapper.toType(d.target()));
        e.setTargetId(targetMapper.toIdOrNull(d.target()));
        e.setEnabled(d.enabled());
        e.setStartsAt(d.startsAt());
        e.setEndsAt(d.endsAt());
        e.setParamsOverride(d.paramsOverride());
        e.setAppliesToOverride(d.appliesToOverride());
        e.setDeletedAt(d.deletedAt());
        return e;
    }
}
