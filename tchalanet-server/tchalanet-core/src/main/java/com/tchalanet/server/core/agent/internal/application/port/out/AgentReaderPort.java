package com.tchalanet.server.core.agent.internal.application.port.out;

import com.tchalanet.server.common.types.id.AgentId;
import com.tchalanet.server.common.types.id.AgentZoneId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.agent.internal.domain.model.Agent;
import com.tchalanet.server.core.agent.internal.domain.model.AgentZone;
import java.util.List;
import java.util.Optional;

public interface AgentReaderPort {
  Optional<Agent> findAgent(TenantId tenantId, AgentId agentId);
  Agent getAgentRequired(TenantId tenantId, AgentId agentId);
  Optional<AgentZone> findZone(TenantId tenantId, AgentZoneId zoneId);
  AgentZone getZoneRequired(TenantId tenantId, AgentZoneId zoneId);
  List<AgentZone> listZones(TenantId tenantId, boolean activeOnly);
  List<Agent> listAgents(TenantId tenantId);
  Optional<Agent> findAgentForSeller(TenantId tenantId, UserId sellerUserId);
  List<AgentId> agentPath(TenantId tenantId, AgentId leafAgentId);
  List<AgentZoneId> zonePath(TenantId tenantId, AgentZoneId leafZoneId);
}
