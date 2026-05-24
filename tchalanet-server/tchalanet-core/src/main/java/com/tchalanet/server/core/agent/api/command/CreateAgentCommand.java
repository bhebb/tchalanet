package com.tchalanet.server.core.agent.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.AgentId;
import com.tchalanet.server.common.types.id.AgentZoneId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.agent.api.model.AgentType;
import com.tchalanet.server.core.agent.api.model.AgentView;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateAgentCommand(
    @NotNull TenantId tenantId,
    AgentId parentAgentId,
    @NotBlank String displayName,
    @NotNull AgentType type,
    @NotNull AgentZoneId primaryZoneId,
    UserId ownerUserId,
    @NotNull List<AgentZoneId> commercialAllowedZoneIds
) implements Command<AgentView> {}
