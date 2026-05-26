package com.tchalanet.server.core.sales.internal.application.service.sell;

import com.tchalanet.server.core.sales.internal.application.service.sell.model.SaleSellerContext;
import com.tchalanet.server.core.session.api.model.ValidatedPosOperationContext;
import org.springframework.stereotype.Component;

@Component
public class SaleSellerContextResolver {

    public SaleSellerContext resolve(ValidatedPosOperationContext pos) {
        // TODO seller-resolution: replace with ResolveSellerForOperationQuery once
        // core.seller is wired. Resolve: sellerId + sellerAssignmentId by userId + outletId + session.
        return SaleSellerContext.empty();
    }
}
