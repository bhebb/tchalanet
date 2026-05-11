package com.tchalanet.server.core.sales.application.port.out;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import java.math.BigDecimal;

public interface AutonomyApprovalPort {
  AutonomyDecision evaluate(OutletId outletId, TerminalId terminalId, UserId sellerUserId, BigDecimal totalAmount);

  record AutonomyDecision(boolean approvalRequired, String approvalRole) {}
}

