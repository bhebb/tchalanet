package com.tchalanet.server.core.agent.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.AgentId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AssignUserToAgentCommand(
    @NotNull TenantId tenantId,
    @NotNull AgentId agentId,
    @NotNull UserId userId,
    @NotBlank String relation
) implements Command<Void> {}
