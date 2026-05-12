package com.tchalanet.server.core.sales.internal.application.port.out;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import java.math.BigDecimal;

public interface LimitPolicyPort {
  LimitDecision evaluateSale(OutletId outletId, TerminalId terminalId, UserId sellerUserId, BigDecimal stakeAmount, BigDecimal totalAmount);

  record LimitDecision(boolean blocked, boolean approvalRequired, String reason) {}
}

