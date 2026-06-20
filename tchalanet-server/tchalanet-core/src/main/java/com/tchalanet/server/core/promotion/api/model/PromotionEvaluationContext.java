package com.tchalanet.server.core.promotion.api.model;

import com.tchalanet.server.common.types.id.AgentId;
import com.tchalanet.server.common.types.id.AgentZoneId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PromotionEvaluationContext(
    TenantId tenantId,
    PromotionEvaluationPhase phase,
    Instant evaluatedAt,
    AgentId agentId,
    List<AgentId> agentPath,
    AgentZoneId zoneId,
    List<AgentZoneId> zonePath,
    UserId sellerUserId,
    BigDecimal paidTotal,
    String currency,
    List<String> paidGameCodes,
    boolean offline
) {
    public PromotionEvaluationContext {
        agentPath = agentPath == null ? List.of() : List.copyOf(agentPath);
        zonePath = zonePath == null ? List.of() : List.copyOf(zonePath);
        paidGameCodes = paidGameCodes == null ? List.of() : List.copyOf(paidGameCodes);
    }
}
