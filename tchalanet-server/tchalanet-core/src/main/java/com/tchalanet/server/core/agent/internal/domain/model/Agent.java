package com.tchalanet.server.core.agent.internal.domain.model;

import com.tchalanet.server.common.types.id.AgentId;
import com.tchalanet.server.common.types.id.AgentZoneId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.agent.api.model.AgentStatus;
import com.tchalanet.server.core.agent.api.model.AgentType;
import java.time.Instant;
import java.util.List;

public record Agent(
    AgentId id,
    TenantId tenantId,
    AgentId parentAgentId,
    String displayName,
    AgentType type,
    AgentStatus status,
    AgentZoneId primaryZoneId,
    UserId ownerUserId,
    int depth,
    List<AgentZoneMandate> mandates,
    Instant createdAt,
    Instant updatedAt
) {
  public static final int MAX_V1_DEPTH = 2;

  public Agent {
    if (id == null) throw new IllegalArgumentException("agent.id_required");
    if (tenantId == null) throw new IllegalArgumentException("agent.tenant_required");
    if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("agent.display_name_required");
    if (type == null) throw new IllegalArgumentException("agent.type_required");
    if (status == null) throw new IllegalArgumentException("agent.status_required");
    if (primaryZoneId == null) throw new IllegalArgumentException("agent.primary_zone_required");
    if (depth < 0 || depth > MAX_V1_DEPTH) throw new IllegalArgumentException("agent.depth_not_supported_v1");
    mandates = mandates == null ? List.of() : List.copyOf(mandates);
  }

  public boolean activeForSale() {
    return status == AgentStatus.ACTIVE;
  }

  public Agent withStatus(AgentStatus newStatus, Instant updatedAt) {
    return new Agent(id, tenantId, parentAgentId, displayName, type, newStatus, primaryZoneId, ownerUserId, depth, mandates, createdAt, updatedAt);
  }

  public static Agent create(
      AgentId id,
      TenantId tenantId,
      AgentId parentAgentId,
      String displayName,
      AgentType type,
      AgentStatus status,
      AgentZoneId primaryZoneId,
      UserId ownerUserId,
      int depth,
      java.util.List<AgentZoneMandate> mandates,
      Instant createdAt
  ) {
    return new Agent(id, tenantId, parentAgentId, displayName, type, status, primaryZoneId, ownerUserId, depth, mandates, createdAt, createdAt);
  }
}
