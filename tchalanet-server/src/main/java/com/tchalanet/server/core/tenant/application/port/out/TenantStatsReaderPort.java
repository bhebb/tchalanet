package com.tchalanet.server.core.tenant.application.port.out;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public interface TenantStatsReaderPort {
    Map<String,Object> getDashboardStats(UUID tenantId, LocalDate since);
}

