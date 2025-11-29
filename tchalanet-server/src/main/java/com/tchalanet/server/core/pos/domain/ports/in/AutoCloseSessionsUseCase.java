package com.tchalanet.server.core.pos.domain.ports.in;

import java.util.UUID;

public interface AutoCloseSessionsUseCase {
  /**
   * Automatically closes POS sessions based on configured rules (e.g., idle timeout, max duration).
   *
   * @param tenantId Optional: if provided, only closes sessions for this tenant.
   * @return The number of sessions automatically closed.
   */
  int autoCloseSessions(UUID tenantId);
}
