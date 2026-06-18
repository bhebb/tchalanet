package com.tchalanet.server.features.tenantadmin.commission.model;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalStatus;

import java.math.BigDecimal;

public record SellerTerminalCommissionRow(
    SellerTerminalId sellerTerminalId,
    String terminalCode,
    String displayName,
    SellerTerminalStatus status,
    BigDecimal commissionRate,
    CommissionRateSource rateSource
) {
    public enum CommissionRateSource { DEFAULT, CUSTOM }
}
