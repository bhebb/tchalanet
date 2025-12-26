package com.tchalanet.server.features.stats.cashier_dashboard.application;

import java.util.Optional;
import java.util.UUID;

public interface CashierReadRepository {
  Optional<CashierInfoProjection> findInfoById(UUID tenantId, UUID cashierId);
}
