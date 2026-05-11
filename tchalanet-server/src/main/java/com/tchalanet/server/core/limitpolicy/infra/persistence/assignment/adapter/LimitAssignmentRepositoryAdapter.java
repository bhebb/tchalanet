package com.tchalanet.server.core.limitpolicy.infra.persistence.assignment.adapter;

import com.tchalanet.server.common.types.enums.RuleKey;
import com.tchalanet.server.common.types.id.LimitAssignmentId;
import com.tchalanet.server.core.limitpolicy.application.port.out.assignment.LimitAssignmentReaderPort;
import com.tchalanet.server.core.limitpolicy.application.port.out.assignment.LimitAssignmentWriterPort;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitAssignment;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitScopeRef;
import com.tchalanet.server.core.limitpolicy.infra.persistence.assignment.LimitAssignmentJpaEntity;
import com.tchalanet.server.core.limitpolicy.infra.persistence.assignment.LimitAssignmentJpaRepository;
import com.tchalanet.server.core.limitpolicy.infra.persistence.assignment.mapper.LimitAssignmentMapper;
import com.tchalanet.server.core.limitpolicy.infra.persistence.assignment.mapper.LimitScopeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LimitAssignmentRepositoryAdapter
    implements LimitAssignmentReaderPort, LimitAssignmentWriterPort {

    private final LimitAssignmentJpaRepository repo;
    private final LimitAssignmentMapper mapper;
    private final LimitScopeMapper scopeMapper;

    @Override
    public Optional<LimitAssignment> findById(LimitAssignmentId id) {
        return repo.findById(id.value())
            .map(mapper::toDomain);
    }

    @Override
    public Optional<LimitAssignment> findByNaturalKey(
        LimitScopeRef scope,
        RuleKey ruleKey
    ) {
        var scopeType = scopeMapper.toType(scope);
        var scopeId = scopeMapper.toId(scope);

        return repo.findActiveByRuleKeyAndScope(
                ruleKey,
                scopeType,
                scopeId)
            .map(mapper::toDomain);
    }

    @Override
    public List<LimitAssignment> listActiveForTargets(
        List<LimitScopeRef> scopes,
        Instant now
    ) {
        if (scopes == null || scopes.isEmpty()) {
            return List.of();
        }

        Map<UUID, LimitAssignmentJpaEntity> byId = new LinkedHashMap<>();

        for (var scope : scopes) {
            var scopeType = scopeMapper.toType(scope);
            var scopeId = scopeMapper.toId(scope);

            var rows = repo.findAllActiveByScope(scopeType, scopeId);

            for (var row : rows) {
                byId.putIfAbsent(row.getId(), row);
            }
        }

        return byId.values().stream()
            .map(mapper::toDomain)
            .filter(assignment -> assignment.isActiveAt(now))
            .toList();
    }

    @Override
    public List<LimitAssignment> listByTarget(LimitScopeRef scope) {
        var scopeType = scopeMapper.toType(scope);
        var scopeId = scopeMapper.toId(scope);

        return repo.findAllByScope(scopeType, scopeId).stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public LimitAssignment save(LimitAssignment assignment) {
        var saved = repo.save(mapper.toEntity(assignment));
        return mapper.toDomain(saved);
    }

    @Override
    public void softDelete(
        LimitAssignmentId id,
        Instant deletedAt
    ) {
        var entity = repo.findById(id.value())
            .orElseThrow(() -> new IllegalArgumentException("limit assignment not found"));

        entity.setDeletedAt(deletedAt);
        repo.save(entity);
    }
}
