package com.tchalanet.server.core.terminal.internal.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalAssignmentId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.terminal.internal.application.port.out.assignment.TerminalAssignmentReaderPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.assignment.TerminalAssignmentWriterPort;
import com.tchalanet.server.core.terminal.internal.domain.model.assignment.TerminalAssignment;
import com.tchalanet.server.core.terminal.internal.domain.model.assignment.TerminalAssignmentStatus;
import com.tchalanet.server.core.terminal.internal.infra.persistence.TerminalAssignmentJpaEntity;
import com.tchalanet.server.core.terminal.internal.infra.persistence.TerminalAssignmentJpaRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaTerminalAssignmentAdapter implements TerminalAssignmentReaderPort, TerminalAssignmentWriterPort {

    private final TerminalAssignmentJpaRepository repository;

    @Override
    public Optional<TerminalAssignment> findActive(TenantId tenantId, TerminalId terminalId, UserId userId) {
        return repository.findByTenantIdAndTerminalIdAndUserIdAndStatus(
            tenantId.value(),
            terminalId.value(),
            userId.value(),
            TerminalAssignmentStatus.ACTIVE
        ).map(this::toDomain);
    }

    @Override
    public TerminalAssignment save(TerminalAssignment assignment) {
        return toDomain(repository.save(toEntity(assignment)));
    }

    private TerminalAssignment toDomain(TerminalAssignmentJpaEntity entity) {
        return new TerminalAssignment(
            TerminalAssignmentId.of(entity.getId()),
            TenantId.of(entity.getTenantId()),
            TerminalId.of(entity.getTerminalId()),
            UserId.of(entity.getUserId()),
            entity.getStatus(),
            entity.getAssignedAt(),
            entity.getRevokedAt()
        );
    }

    private TerminalAssignmentJpaEntity toEntity(TerminalAssignment assignment) {
        var entity = new TerminalAssignmentJpaEntity();
        entity.setId(assignment.id().value());
        entity.setTenantId(assignment.tenantId().value());
        entity.setTerminalId(assignment.terminalId().value());
        entity.setUserId(assignment.userId().value());
        entity.setStatus(assignment.status());
        entity.setAssignedAt(assignment.assignedAt());
        entity.setRevokedAt(assignment.revokedAt());
        return entity;
    }
}
