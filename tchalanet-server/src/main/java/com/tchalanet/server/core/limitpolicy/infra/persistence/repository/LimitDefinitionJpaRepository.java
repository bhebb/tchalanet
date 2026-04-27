package com.tchalanet.server.core.limitpolicy.infra.persistence.repository;

import com.tchalanet.server.common.types.enums.RuleKey;
import com.tchalanet.server.core.limitpolicy.infra.persistence.entity.LimitDefinitionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LimitDefinitionJpaRepository
    extends JpaRepository<LimitDefinitionJpaEntity, UUID> {

    Optional<LimitDefinitionJpaEntity> findByRuleKey(RuleKey ruleKey);

    List<LimitDefinitionJpaEntity> findAllByEnabledIsTrueOrderByRuleKeyAsc();
}
