package com.tchalanet.server.core.agent.internal.application.port.out;

import com.tchalanet.server.common.types.id.AgentId;
import com.tchalanet.server.common.types.id.AgentZoneId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.agent.internal.domain.model.Agent;
import com.tchalanet.server.core.agent.internal.domain.model.AgentZone;

public interface AgentWriterPort {
  AgentZone saveZone(AgentZone zone);
  boolean zoneCodeExists(TenantId tenantId, String code);
  Agent saveAgent(Agent agent);
  void assignUser(TenantId tenantId, AgentId agentId, UserId userId, String relation);
  boolean userAssignmentExists(TenantId tenantId, AgentId agentId, UserId userId, String relation);
}
