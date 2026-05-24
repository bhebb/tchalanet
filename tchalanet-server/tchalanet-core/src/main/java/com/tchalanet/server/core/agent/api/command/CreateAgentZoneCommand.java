package com.tchalanet.server.core.agent.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.AgentZoneId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.agent.api.model.AgentZoneView;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAgentZoneCommand(
    @NotNull TenantId tenantId,
    AgentZoneId parentZoneId,
    @NotBlank String code,
    @NotBlank String name,
    @NotBlank String zoneType
) implements Command<AgentZoneView> {}
