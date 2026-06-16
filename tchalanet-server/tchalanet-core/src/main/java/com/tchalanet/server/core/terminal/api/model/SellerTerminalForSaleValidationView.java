package com.tchalanet.server.core.terminal.api.model;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.terminal.internal.domain.model.sellerterminal.SellerTerminal;

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

    public static SellerTerminalForSaleValidationView from(SellerTerminal t) {
        return new SellerTerminalForSaleValidationView(
            t.id(), t.tenantId(), t.status(), t.commissionRate());
    }
}
