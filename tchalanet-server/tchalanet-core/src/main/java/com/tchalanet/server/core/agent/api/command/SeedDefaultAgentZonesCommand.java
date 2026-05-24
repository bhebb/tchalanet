package com.tchalanet.server.core.agent.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.agent.api.model.AgentZoneView;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SeedDefaultAgentZonesCommand(@NotNull TenantId tenantId) implements Command<List<AgentZoneView>> {}
