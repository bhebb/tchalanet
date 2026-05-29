package com.tchalanet.server.core.seller.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.seller.api.query.model.SellerSummaryView;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ListSellersQuery(
    @NotNull TenantId tenantId
) implements Query<List<SellerSummaryView>> {}
