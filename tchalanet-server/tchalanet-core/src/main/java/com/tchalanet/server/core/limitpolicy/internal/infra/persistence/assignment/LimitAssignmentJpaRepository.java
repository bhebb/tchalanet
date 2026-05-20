package com.tchalanet.server.core.limitpolicy.internal.infra.persistence.assignment;

import com.tchalanet.server.core.limitpolicy.api.RuleKey;
import com.tchalanet.server.core.limitpolicy.api.ScopeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LimitAssignmentJpaRepository
    extends JpaRepository<LimitAssignmentJpaEntity, UUID> {

    @Query("""
      select e
      from LimitAssignmentJpaEntity e
      where e.ruleKey = :ruleKey
        and e.scopeType = :scopeType
        and e.scopeId = :scopeId
        and e.deletedAt is null
      """)
    Optional<LimitAssignmentJpaEntity> findActiveByRuleKeyAndScope(
        RuleKey ruleKey,
        ScopeType scopeType,
        UUID scopeId
    );

    @Query("""
      select e
      from LimitAssignmentJpaEntity e
      where e.scopeType = :scopeType
        and e.scopeId = :scopeId
        and e.deletedAt is null
      """)
    List<LimitAssignmentJpaEntity> findAllActiveByScope(
        ScopeType scopeType,
        UUID scopeId
    );

    @Query("""
      select e
      from LimitAssignmentJpaEntity e
      where e.scopeType = :scopeType
        and e.scopeId = :scopeId
      """)
    List<LimitAssignmentJpaEntity> findAllByScope(
        ScopeType scopeType,
        UUID scopeId
    );
}
