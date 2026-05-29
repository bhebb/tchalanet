package com.tchalanet.server.core.seller.api.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SellerId;
import com.tchalanet.server.common.types.id.SellerOutletAssignmentId;

public record SellerOperationalView(
    SellerId sellerId,
    SellerOutletAssignmentId assignmentId,
    OutletId outletId,
    SellerStatus sellerStatus,
    SellerAssignmentStatus assignmentStatus,
    boolean eligibleForSale
) {}
