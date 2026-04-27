package com.tchalanet.server.core.session.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.SessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.session.domain.model.PosSession;
import java.math.BigDecimal;

/** Command to close a POS session. */
public record CloseSessionCommand(TenantId tenantId, SessionId sessionId, BigDecimal closingAmount)
    implements Command<PosSession> {

  public static CloseSessionCommand of(
      TenantId tenantId, SessionId sessionId, BigDecimal closingAmount) {
    return new CloseSessionCommand(tenantId, sessionId, closingAmount);
  }
}
