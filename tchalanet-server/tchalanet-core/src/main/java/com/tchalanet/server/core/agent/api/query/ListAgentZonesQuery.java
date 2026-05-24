package com.tchalanet.server.core.agent.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.agent.api.model.AgentZoneView;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ListAgentZonesQuery(@NotNull TenantId tenantId, boolean activeOnly) implements Query<List<AgentZoneView>> {}
