package com.tchalanet.server.core.limitpolicy.infra.persistence.adapter;

import com.tchalanet.server.core.limitpolicy.application.port.out.LimitAssignmentReaderPort;
import com.tchalanet.server.core.limitpolicy.application.port.out.LimitAssignmentWriterPort;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitAssignment;
import com.tchalanet.server.core.limitpolicy.infra.persistence.mapper.LimitAssignmentMapper;
import com.tchalanet.server.core.limitpolicy.infra.persistence.repository.LimitAssignmentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class LimitAssignmentRepositoryAdapter implements LimitAssignmentReaderPort, LimitAssignmentWriterPort {

    private final LimitAssignmentJpaRepository jpaRepository;
    private final LimitAssignmentMapper mapper;

    @Override
    public List<LimitAssignment> findActiveByTarget(UUID tenantId, String targetType, UUID targetId) {
        return jpaRepository.findByTenantIdAndTargetTypeAndTargetIdAndDeletedAtIsNull(tenantId, targetType, targetId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<LimitAssignment> findById(UUID tenantId, UUID assignmentId) {
        return jpaRepository.findById(assignmentId)
                .filter(e -> e.getTenantId().equals(tenantId) && e.getDeletedAt() == null)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByTenantAndLimitAndTarget(UUID tenantId, UUID limitDefinitionId, String targetType, UUID targetId) {
        return jpaRepository.existsByTenantIdAndLimitDefinitionIdAndTargetTypeAndTargetIdAndDeletedAtIsNull(
                tenantId, limitDefinitionId, targetType, targetId);
    }

    @Override
    public LimitAssignment save(LimitAssignment assignment) {
        var entity = mapper.toEntity(assignment);
        entity = jpaRepository.save(entity);
        return mapper.toDomain(entity);
    }

    @Override
    public void softDelete(UUID tenantId, UUID assignmentId) {
        var entity = jpaRepository.findById(assignmentId).orElseThrow();
        if (!entity.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Assignment not found");
        }
        entity.setDeletedAt(Instant.now());
        jpaRepository.save(entity);
    }
}
