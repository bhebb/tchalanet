package com.tchalanet.server.core.sales.domain.service;

import java.math.BigDecimal;

public class TicketMoneyPolicy {

  public BigDecimal total(BigDecimal stakeAmount, BigDecimal feeAmount) {
    var safeStake = stakeAmount == null ? BigDecimal.ZERO : stakeAmount;
    var safeFee = feeAmount == null ? BigDecimal.ZERO : feeAmount;
    return safeStake.add(safeFee);
  }
}

