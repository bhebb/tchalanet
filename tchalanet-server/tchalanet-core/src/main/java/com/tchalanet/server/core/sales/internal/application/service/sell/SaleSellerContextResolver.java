package com.tchalanet.server.core.sales.internal.application.service.sell;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.core.sales.internal.application.service.sell.model.SaleSellerContext;
import com.tchalanet.server.core.seller.api.query.ResolveSellerForOperationQuery;
import com.tchalanet.server.core.session.api.model.ValidatedPosOperationContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SaleSellerContextResolver {

    private final QueryBus queryBus;

    public SaleSellerContext resolve(ValidatedPosOperationContext pos) {
        var seller = queryBus.ask(new ResolveSellerForOperationQuery(
            pos.tenantId(),
            pos.actorUserId(),
            pos.outletId(),
            pos.salesSessionId()
        ));

        return new SaleSellerContext(seller.sellerId(), seller.assignmentId());
    }
}
