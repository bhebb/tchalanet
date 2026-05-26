package com.tchalanet.server.core.seller.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.SellerId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.seller.api.model.SellerView;
import jakarta.validation.constraints.NotNull;

public record GetSellerQuery(
    @NotNull TenantId tenantId,
    @NotNull SellerId sellerId
) implements Query<SellerView> {}
