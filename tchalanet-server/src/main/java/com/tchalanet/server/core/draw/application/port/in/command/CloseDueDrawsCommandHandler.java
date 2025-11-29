package com.tchalanet.server.core.draw.application.port.in.command;

import java.util.UUID;

public interface CloseDueDrawsCommandHandler {
  /** Close due draws for the provided tenantId. */
  void handle(UUID tenantId);
}
