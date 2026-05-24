package com.tchalanet.server.core.sales.internal.application.service.sell.model;

import com.tchalanet.server.common.types.id.AgentId;
import com.tchalanet.server.common.types.id.AgentZoneId;
import java.util.List;

public record SaleAgentPromotionContext(
    AgentId agentId,
    List<AgentId> agentPath,
    AgentZoneId zoneId,
    List<AgentZoneId> zonePath
) {

    public SaleAgentPromotionContext {
        agentPath = agentPath == null ? List.of() : List.copyOf(agentPath);
        zonePath = zonePath == null ? List.of() : List.copyOf(zonePath);
    }

    public static SaleAgentPromotionContext empty() {
        return new SaleAgentPromotionContext(null, List.of(), null, List.of());
    }
}
