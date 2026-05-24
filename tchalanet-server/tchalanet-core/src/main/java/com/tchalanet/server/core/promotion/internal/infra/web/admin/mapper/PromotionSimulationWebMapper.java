package com.tchalanet.server.core.promotion.internal.infra.web.admin.mapper;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.promotion.api.model.PromotionEvaluationContext;
import com.tchalanet.server.core.promotion.internal.infra.web.admin.request.SimulatePromotionRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class PromotionSimulationWebMapper {

    public PromotionEvaluationContext toEvaluationContext(
        TenantId tenantId,
        SimulatePromotionRequest request
    ) {
        return new PromotionEvaluationContext(
            tenantId,
            request.phase(),
            request.at() == null ? Instant.now() : request.at(),
            request.agentId(),
            request.agentPath() == null ? List.of() : request.agentPath(),
            request.zoneId(),
            request.zonePath() == null ? List.of() : request.zonePath(),
            request.outletId(),
            request.terminalId(),
            request.salesSessionId(),
            request.sellerUserId(),
            request.paidTotal(),
            request.currency(),
            request.paidGameCodes() == null ? List.of() : request.paidGameCodes(),
            request.offline()
        );
    }
}

