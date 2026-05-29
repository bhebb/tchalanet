package com.tchalanet.server.core.sales.internal.application.service.sell.model;

import com.tchalanet.server.common.types.id.SellerId;
import com.tchalanet.server.common.types.id.SellerOutletAssignmentId;

public record SaleSellerContext(
    SellerId sellerId,
    SellerOutletAssignmentId sellerAssignmentId
) {
    public static SaleSellerContext empty() {
        return new SaleSellerContext(null, null);
    }
}
