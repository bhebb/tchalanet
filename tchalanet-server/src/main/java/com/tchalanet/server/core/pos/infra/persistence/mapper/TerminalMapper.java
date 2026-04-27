package com.tchalanet.server.core.pos.infra.persistence.mapper;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.core.pos.domain.model.Terminal;
import com.tchalanet.server.core.pos.infra.persistence.TerminalJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class TerminalMapper {

  private final JsonUtils jsonUtils;

  public TerminalMapper(JsonUtils jsonUtils) {
    this.jsonUtils = jsonUtils;
  }

  public Terminal toDomain(TerminalJpaEntity entity) {
    return new Terminal(
        entity.getId(),
        TenantId.nullableOf(entity.getTenantId()),
        OutletId.nullableOf(entity.getOutletId()),
        Terminal.TerminalState.valueOf(entity.getState()),
        entity.getLastSeen(),
        // convert JsonNode -> String
        entity.getMetadataJson() == null ? null : jsonUtils.toJson(entity.getMetadataJson()),
        entity.getVersion(),
        entity.getRegisteredAt(),
        entity.getUnregisteredAt(),
        entity.getLockedAt(),
        entity.getLockedBy(),
        entity.getLockReason(),
        entity.getDeletedAt(),
        entity.getLabel(),
        entity.getInventoryTag());
  }

  public TerminalJpaEntity toEntity(Terminal domain) {
    TerminalJpaEntity entity = new TerminalJpaEntity();
    entity.setId(domain.id());
    entity.setTenantId(domain.tenantId().uuid());
    entity.setOutletId(domain.outletId().uuid());
    entity.setState(domain.state().name());
    entity.setLastSeen(domain.lastSeen());
    // convert String -> JsonNode; if domain.meta() is null use empty object
    entity.setMetadataJson(domain.meta() == null ? jsonUtils.emptyObjectNode() : jsonUtils.parse(domain.meta()));
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
