package com.tchalanet.server.core.limitpolicy.infra.persistence.mapper;

import com.tchalanet.server.common.types.id.LimitDefinitionId;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitDefinition;
import com.tchalanet.server.core.limitpolicy.infra.persistence.entity.LimitDefinitionJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class LimitDefinitionMapper {

    public LimitDefinition toDomain(LimitDefinitionJpaEntity e) {
        return new LimitDefinition(
            LimitDefinitionId.of(e.getId()),
            e.getRuleKey(),
            e.isEnabled(),
            e.getOnBreach(),
            e.getParams(),
            e.getAppliesTo(),
            e.getDeletedAt()
        );
    }

    public LimitDefinitionJpaEntity toEntity(LimitDefinition d) {
        var e = new LimitDefinitionJpaEntity();
        e.setId(d.id().value());
        e.setRuleKey(d.ruleKey());
        e.setEnabled(d.enabled());
        e.setOnBreach(d.onBreach());
        e.setParams(d.params());
        e.setAppliesTo(d.appliesTo());
        e.setDeletedAt(d.deletedAt());
        return e;
    }
}
