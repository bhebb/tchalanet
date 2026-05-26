package com.tchalanet.server.core.seller.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.seller.api.model.SellerOperationalView;
import jakarta.validation.constraints.NotNull;

public record ResolveSellerForOperationQuery(
    @NotNull TenantId tenantId,
    @NotNull UserId userId,
    @NotNull OutletId outletId,
    SalesSessionId sessionId
) implements Query<SellerOperationalView> {}
