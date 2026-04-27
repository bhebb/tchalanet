package com.tchalanet.server.core.tenantuser.application.port.out;

import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.OutletId;

import java.time.Instant;

public interface TerminalReaderPort {
  TerminalSummary getRequired(TerminalId terminalId);

  record TerminalSummary(
      TerminalId terminalId,
      OutletId outletId,
      Instant lockedAt,
      Instant unregisteredAt,
      String state) {}
}
