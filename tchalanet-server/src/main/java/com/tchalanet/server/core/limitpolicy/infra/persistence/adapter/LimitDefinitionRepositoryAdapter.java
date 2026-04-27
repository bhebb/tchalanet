package com.tchalanet.server.core.limitpolicy.infra.persistence.adapter;

import com.tchalanet.server.common.types.enums.RuleKey;
import com.tchalanet.server.common.types.id.LimitDefinitionId;
import com.tchalanet.server.core.limitpolicy.application.port.out.LimitDefinitionReaderPort;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitDefinition;
import com.tchalanet.server.core.limitpolicy.infra.persistence.mapper.LimitDefinitionMapper;
import com.tchalanet.server.core.limitpolicy.infra.persistence.repository.LimitDefinitionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LimitDefinitionRepositoryAdapter
    implements LimitDefinitionReaderPort {

    private final LimitDefinitionJpaRepository repo;
    private final LimitDefinitionMapper mapper;

    @Override
    public Optional<LimitDefinition> findById(LimitDefinitionId id) {
        return repo.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<LimitDefinition> findByRuleKey(RuleKey ruleKey) {
        return repo.findByRuleKey(ruleKey).map(mapper::toDomain);
    }

    @Override
    public List<LimitDefinition> listActive() {
        return repo.findAllByEnabledIsTrueOrderByRuleKeyAsc().stream().map(mapper::toDomain).toList();
    }
}
