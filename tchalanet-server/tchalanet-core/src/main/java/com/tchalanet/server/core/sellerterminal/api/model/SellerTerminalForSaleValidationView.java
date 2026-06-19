package com.tchalanet.server.core.sellerterminal.api.model;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;

import java.math.BigDecimal;

public record SellerTerminalForSaleValidationView(
    SellerTerminalId id,
    TenantId tenantId,
    SellerTerminalStatus status,
    BigDecimal commissionRate
) {
    public boolean canSell() {
        return status == SellerTerminalStatus.ACTIVE;
    }
}
