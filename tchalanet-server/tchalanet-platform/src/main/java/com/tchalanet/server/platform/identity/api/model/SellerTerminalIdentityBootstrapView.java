package com.tchalanet.server.platform.identity.api.model;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;

public record SellerTerminalIdentityBootstrapView(
    SellerTerminalId sellerTerminalId,
    TenantId tenantId,
    SellerTerminalBootstrapStatus status
) {
    public boolean isActive() {
        return status == SellerTerminalBootstrapStatus.ACTIVE;
    }
}
