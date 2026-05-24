package com.tchalanet.server.core.agent.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.AgentId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.agent.api.model.AgentView;
import jakarta.validation.constraints.NotNull;

public record GetAgentViewQuery(@NotNull TenantId tenantId, @NotNull AgentId agentId) implements Query<AgentView> {}
