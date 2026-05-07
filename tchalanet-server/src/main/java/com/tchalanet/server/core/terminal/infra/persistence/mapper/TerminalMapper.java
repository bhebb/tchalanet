package com.tchalanet.server.core.terminal.infra.persistence.mapper;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.core.terminal.domain.model.Terminal;
import com.tchalanet.server.core.terminal.domain.model.TerminalKind;
import com.tchalanet.server.core.terminal.domain.model.TerminalState;
import com.tchalanet.server.core.terminal.domain.model.TerminalSyncState;
import com.tchalanet.server.core.terminal.infra.persistence.TerminalJpaEntity;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class TerminalMapper {

  private final JsonUtils jsonUtils;

  public TerminalMapper(JsonUtils jsonUtils) {
    this.jsonUtils = jsonUtils;
  }

  public Terminal toDomain(TerminalJpaEntity e) {
    return new Terminal(
        TerminalId.of(e.getId()),
        TenantId.nullableOf(e.getTenantId()),
        OutletId.nullableOf(e.getOutletId()),
        UserId.nullableOf(e.getAssignedUserId()),
        e.getKind() == null ? TerminalKind.PHYSICAL : TerminalKind.valueOf(e.getKind()),
        TerminalState.valueOf(e.getState()),
        e.isActiveForUser(),
        e.getSyncState() == null
            ? TerminalSyncState.ONLINE
            : TerminalSyncState.valueOf(e.getSyncState()),
        e.getLastSeen(),
        e.getLabel(),
        e.getInventoryTag(),
        toMetadata(e.getMetadataJson()),
        e.getRegisteredAt(),
        e.getUnregisteredAt(),
        e.getLockedAt(),
        UserId.nullableOf(e.getLockedBy()),
        e.getLockReason());
  }

  public TerminalJpaEntity toEntity(Terminal d) {
    TerminalJpaEntity e = new TerminalJpaEntity();
    applyToEntity(d, e);
    return e;
  }

  public void applyToEntity(Terminal d, TerminalJpaEntity e) {
    e.setId(d.id().value());
    e.setTenantId(d.tenantId() == null ? null : d.tenantId().uuid());
    e.setOutletId(d.outletId() == null ? null : d.outletId().uuid());
    e.setAssignedUserId(d.assignedUserId() == null ? null : d.assignedUserId().value());
    e.setKind(d.kind().name());
    e.setState(d.state().name());
    e.setActiveForUser(d.activeForUser());
    e.setSyncState(d.syncState().name());
    e.setLastSeen(d.lastSeen());
    e.setLabel(d.label());
    e.setInventoryTag(d.inventoryTag());
    e.setMetadataJson(toJsonNode(d.metadata()));
    e.setRegisteredAt(d.registeredAt());
    e.setUnregisteredAt(d.unregisteredAt());
    e.setLockedAt(d.lockedAt());
    e.setLockedBy(d.lockedBy() == null ? null : d.lockedBy().value());
    e.setLockReason(d.lockReason());
  }

  private Map<String, Object> toMetadata(JsonNode node) {
    if (node == null || node.isNull()) return Map.of();
    Map<?, ?> raw = jsonUtils.treeToValue(node, Map.class);
    if (raw == null || raw.isEmpty()) return Map.of();
    Map<String, Object> result = new HashMap<>();
    raw.forEach((k, v) -> result.put(String.valueOf(k), v));
    return Map.copyOf(result);
  }

  private JsonNode toJsonNode(Map<String, Object> metadata) {
    if (metadata == null || metadata.isEmpty()) return jsonUtils.emptyObjectNode();
    return jsonUtils.valueToTree(metadata);
  }
}
