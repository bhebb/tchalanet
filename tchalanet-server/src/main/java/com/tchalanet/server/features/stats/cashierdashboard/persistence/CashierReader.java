package com.tchalanet.server.features.stats.cashierdashboard.persistence;

import com.tchalanet.server.features.stats.cashierdashboard.model.CashierInfoProjection;
import java.util.Optional;
import java.util.UUID;

public interface CashierReader {
  Optional<CashierInfoProjection> findInfoById(UUID tenantId, UUID cashierId);
}
