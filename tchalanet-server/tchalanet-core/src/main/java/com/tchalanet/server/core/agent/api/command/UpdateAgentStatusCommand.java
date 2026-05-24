package com.tchalanet.server.core.agent.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.AgentId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.agent.api.model.AgentStatus;
import com.tchalanet.server.core.agent.api.model.AgentView;
import jakarta.validation.constraints.NotNull;

public record UpdateAgentStatusCommand(
    @NotNull TenantId tenantId,
    @NotNull AgentId agentId,
    @NotNull AgentStatus status,
    String reason
) implements Command<AgentView> {}
