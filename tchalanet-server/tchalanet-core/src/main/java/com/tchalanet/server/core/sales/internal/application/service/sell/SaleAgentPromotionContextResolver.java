package com.tchalanet.server.core.sales.internal.application.service.sell;

import com.tchalanet.server.core.sales.internal.application.service.sell.model.SaleAgentPromotionContext;
import com.tchalanet.server.core.session.api.model.ValidatedPosOperationContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SaleAgentPromotionContextResolver {

    public SaleAgentPromotionContext resolve(ValidatedPosOperationContext pos) {
        // TODO promotionDecision-agent:
        // Replace empty context with core.agent API lookup once available.
        //
        // Expected future flow:
        // - find agent assignment by tenant/user/outlet/session
        // - resolve agentId
        // - resolve agentPath
        // - resolve zoneId
        // - resolve zonePath
        return SaleAgentPromotionContext.empty();
    }
}
