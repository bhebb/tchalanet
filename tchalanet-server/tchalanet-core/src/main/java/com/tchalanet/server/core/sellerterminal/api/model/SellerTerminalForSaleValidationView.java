package com.tchalanet.server.core.sellerterminal.api.model;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import java.math.BigDecimal;

public record SellerTerminalForSaleValidationView(
    SellerTerminalId id,
    TenantId tenantId,
    SellerTerminalStatus status,
    BigDecimal commissionRate,
    boolean mustChangePin) {
  public boolean canSell() {
    return canSell(true);
  }

  public boolean canSell(boolean requirePinChangeCompleted) {
    return status == SellerTerminalStatus.ACTIVE && (!requirePinChangeCompleted || !mustChangePin);
  }
}
