package com.tchalanet.server.core.pos.infra.persistence.mapper;

import com.tchalanet.server.core.pos.domain.model.Terminal;
import com.tchalanet.server.core.pos.infra.persistence.TerminalJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class TerminalMapper {

  public Terminal toDomain(TerminalJpaEntity entity) {
    return new Terminal(
        entity.getId(),
        entity.getTenantId(),
        entity.getOutletId(),
        Terminal.TerminalState.valueOf(entity.getState()),
        entity.getLastSeen(),
        entity.getMeta(),
        entity.getVersion(),
        entity.getRegisteredAt(),
        entity.getUnregisteredAt(),
        entity.getLockedAt(),
        entity.getLockedBy(),
        entity.getLockReason(),
        entity.getDeletedAt()
    );
  }

  public TerminalJpaEntity toEntity(Terminal domain) {
    TerminalJpaEntity entity = new TerminalJpaEntity();
    entity.setId(domain.id());
    entity.setTenantId(domain.tenantId());
    entity.setOutletId(domain.outletId());
    entity.setState(domain.state().name());
    entity.setLastSeen(domain.lastSeen());
    entity.setMeta(domain.meta());
    entity.setVersion(domain.version());
    entity.setRegisteredAt(domain.registeredAt());
    entity.setUnregisteredAt(domain.unregisteredAt());
    entity.setLockedAt(domain.lockedAt());
    entity.setLockedBy(domain.lockedBy());
    entity.setLockReason(domain.lockReason());
    entity.setDeletedAt(domain.deletedAt());
    return entity;
  }
}
