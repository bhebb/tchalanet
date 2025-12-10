package com.tchalanet.server.features.stats.cashier_dashboard.application;

import java.util.UUID;

public interface CashierSessionReadRepository {
    boolean hasOpenSession(UUID tenantId, UUID cashierId);
}
