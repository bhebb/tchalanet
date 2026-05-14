package com.tchalanet.server.core.sales.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.LocalDate;
import java.util.Optional;

/** Query to get an agent's daily sales summary */
public record GetAgentDailySalesQuery(TenantId tenantId, UserId agentId, LocalDate date)
    implements Query<Optional<AgentDailySalesDto>> {}
