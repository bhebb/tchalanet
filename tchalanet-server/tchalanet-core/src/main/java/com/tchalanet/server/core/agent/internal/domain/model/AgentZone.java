package com.tchalanet.server.core.agent.internal.domain.model;

import com.tchalanet.server.common.types.id.AgentZoneId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.agent.api.model.AgentZoneStatus;
import java.time.Instant;

public record AgentZone(
    AgentZoneId id,
    TenantId tenantId,
    AgentZoneId parentZoneId,
    String code,
    String name,
    String zoneType,
    AgentZoneStatus status,
    int depth,
    Instant createdAt,
    Instant updatedAt
) {
  public AgentZone {
    if (id == null) throw new IllegalArgumentException("agent_zone.id_required");
    if (tenantId == null) throw new IllegalArgumentException("agent_zone.tenant_required");
    if (code == null || code.isBlank()) throw new IllegalArgumentException("agent_zone.code_required");
    if (name == null || name.isBlank()) throw new IllegalArgumentException("agent_zone.name_required");
    if (status == null) throw new IllegalArgumentException("agent_zone.status_required");
    if (depth < 0) throw new IllegalArgumentException("agent_zone.invalid_depth");
  }

  public boolean active() { return status == AgentZoneStatus.ACTIVE; }
}
