package com.tchalanet.server.features.stats.cashierdashboard.persistence;

import java.util.UUID;

public interface CashierSessionReader {
  boolean hasOpenSession(UUID tenantId, UUID cashierId);
}
