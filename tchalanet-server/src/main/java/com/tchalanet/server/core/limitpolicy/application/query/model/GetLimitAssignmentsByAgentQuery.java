package com.tchalanet.server.core.limitpolicy.application.query.model;

import com.tchalanet.server.common.types.id.AgentId;
import com.tchalanet.server.common.types.id.TenantId;

public record GetLimitAssignmentsByAgentQuery(TenantId tenantId, AgentId agentId) {}
