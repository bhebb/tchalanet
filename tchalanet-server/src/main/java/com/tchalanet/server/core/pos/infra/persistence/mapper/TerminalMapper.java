package com.tchalanet.server.core.pos.infra.persistence.mapper;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.pos.domain.model.Terminal;
import com.tchalanet.server.core.pos.infra.persistence.TerminalJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class TerminalMapper {

  public Terminal toDomain(TerminalJpaEntity entity) {
    var t =
        new Terminal(
            entity.getId(),
            TenantId.nullableOf(entity.getTenantId()),
            OutletId.nullableOf(entity.getOutletId()),
            Terminal.TerminalState.valueOf(entity.getState()),
            entity.getLastSeen(),
            // domain.meta maps to entity.metadataJson
            entity.getMetadataJson(),
            entity.getVersion(),
            entity.getRegisteredAt(),
            entity.getUnregisteredAt(),
            entity.getLockedAt(),
            entity.getLockedBy(),
            entity.getLockReason(),
            entity.getDeletedAt());
    t.setLabel(entity.getLabel());
    t.setInventoryTag(entity.getInventoryTag());
    return t;
  }

  public TerminalJpaEntity toEntity(Terminal domain) {
    TerminalJpaEntity entity = new TerminalJpaEntity();
    entity.setId(domain.id());
    entity.setTenantId(domain.tenantId().uuid());
    entity.setOutletId(domain.outletId().uuid());
    entity.setState(domain.state().name());
    entity.setLastSeen(domain.lastSeen());
    // map domain.meta to entity.metadataJson
    entity.setMetadataJson(domain.meta());
    entity.setLabel(domain.label());
    entity.setInventoryTag(domain.inventoryTag());
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
