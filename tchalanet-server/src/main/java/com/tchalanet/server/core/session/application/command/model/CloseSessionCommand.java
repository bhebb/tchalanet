package com.tchalanet.server.core.session.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.core.session.domain.model.PosSession;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Command to close a POS session.
 */
public record CloseSessionCommand(
    UUID tenantId,
    UUID sessionId,
    BigDecimal closingAmount
) implements Command<PosSession> {

  public static CloseSessionCommand of(UUID tenantId, UUID sessionId, BigDecimal closingAmount) {
    return new CloseSessionCommand(tenantId, sessionId, closingAmount);
  }
}
