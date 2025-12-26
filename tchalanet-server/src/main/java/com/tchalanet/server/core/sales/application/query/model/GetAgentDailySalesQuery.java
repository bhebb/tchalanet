package com.tchalanet.server.core.sales.application.query.model;
import com.tchalanet.server.common.types.id.AgentId;
import com.tchalanet.server.common.types.id.TenantId;

import java.util.UUID;
import java.time.LocalDate;

/** Query to get an agent's daily sales summary */
public record GetAgentDailySalesQuery(
    TenantId tenantId,
    AgentId agentId,
    LocalDate date
) {}

