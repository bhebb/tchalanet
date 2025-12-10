package com.tchalanet.server.core.tenant.application.port.out;

import java.time.YearMonth;
import java.util.UUID;

public interface ReportGeneratorPort {
    String generateTenantMonthlyReport(UUID tenantId, YearMonth month);
}

