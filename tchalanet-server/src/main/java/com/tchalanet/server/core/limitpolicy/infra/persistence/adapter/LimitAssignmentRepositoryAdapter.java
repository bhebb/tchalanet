package com.tchalanet.server.core.limitpolicy.infra.persistence.adapter;

import com.tchalanet.server.common.types.enums.TargetType;
import com.tchalanet.server.common.types.id.LimitAssignmentId;
import com.tchalanet.server.common.types.id.LimitDefinitionId;
import com.tchalanet.server.core.limitpolicy.application.port.out.LimitAssignmentReaderPort;
import com.tchalanet.server.core.limitpolicy.application.port.out.LimitAssignmentWriterPort;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitAssignment;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitTarget;
import com.tchalanet.server.core.limitpolicy.infra.persistence.mapper.LimitAssignmentMapper;
import com.tchalanet.server.core.limitpolicy.infra.persistence.mapper.LimitTargetMapper;
import com.tchalanet.server.core.limitpolicy.infra.persistence.repository.LimitAssignmentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class LimitAssignmentRepositoryAdapter
    implements LimitAssignmentReaderPort, LimitAssignmentWriterPort {

    private final LimitAssignmentJpaRepository repo;
    private final LimitAssignmentMapper mapper;
    private final LimitTargetMapper targetMapper;

    @Override
    public Optional<LimitAssignment> findById(LimitAssignmentId id) {
        return repo.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public List<LimitAssignment> listActiveForTargets(List<LimitTarget> targets, Instant now) {
        // Collect entities for each requested target and tenant-level assignments
        Map<UUID, com.tchalanet.server.core.limitpolicy.infra.persistence.entity.LimitAssignmentJpaEntity> byId = new LinkedHashMap<>();

        // always include tenant-level assignments (target_id IS NULL)
        var tenantEntities = repo.findAllByTargetTypeAndTargetIdIsNull(TargetType.TENANT);
        for (var e : tenantEntities) byId.putIfAbsent(e.getId(), e);

        if (targets != null) {
            for (var t : targets) {
                var tt = targetMapper.toType(t);
                var id = targetMapper.toIdOrNull(t);
                List<com.tchalanet.server.core.limitpolicy.infra.persistence.entity.LimitAssignmentJpaEntity> list;
                if (id == null) {
                    list = repo.findAllByTargetTypeAndTargetIdIsNull(tt);
                } else {
                    list = repo.findAllByTargetTypeAndTargetId(tt, id);
                }
                for (var e : list) byId.putIfAbsent(e.getId(), e);
            }
        }

        // Map to domain and filter active at 'now'
        return byId.values().stream()
            .map(mapper::toDomain)
            .filter(a -> a.appliesTo(a.target(), now) || a.isActiveAt(now)) // fallback: keep if active
            .filter(a -> a.isActiveAt(now))
            .collect(Collectors.toList());
    }

    @Override
    public Optional<LimitAssignment> findByNaturalKey(LimitTarget target, LimitDefinitionId definitionId) {
        var tt = targetMapper.toType(target);
        var id = targetMapper.toIdOrNull(target);
        Optional<com.tchalanet.server.core.limitpolicy.infra.persistence.entity.LimitAssignmentJpaEntity> ent;
        if (id == null) {
            ent = repo.findByLimitDefinitionIdAndTargetTypeAndTargetIdIsNull(definitionId.value(), tt);
        } else {
            ent = repo.findByLimitDefinitionIdAndTargetTypeAndTargetId(definitionId.value(), tt, id);
        }
        return ent.map(mapper::toDomain);
    }

    @Override
    public List<LimitAssignment> listByTarget(LimitTarget target) {
        var tt = targetMapper.toType(target);
        var id = targetMapper.toIdOrNull(target);
        List<com.tchalanet.server.core.limitpolicy.infra.persistence.entity.LimitAssignmentJpaEntity> list;
        if (id == null) {
            list = repo.findAllByTargetTypeAndTargetIdIsNull(tt);
        } else {
            list = repo.findAllByTargetTypeAndTargetId(tt, id);
        }
        return list.stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public LimitAssignment save(LimitAssignment a) {
        var saved = repo.save(mapper.toEntity(a));
        return mapper.toDomain(saved);
    }

    @Override
    public void softDelete(LimitAssignmentId id) {
        repo.deleteById(id.value());
    }

    @Override
    public void softDeleteByDefinitionId(LimitDefinitionId defId) {
        repo.deleteByLimitDefinitionId(defId.value());
    }
}
