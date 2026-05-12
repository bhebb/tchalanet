package com.tchalanet.server.features.reporting.tenantkpis;

import java.time.LocalDate;
import java.util.UUID;

public record TenantKpisCriteria(UUID tenantId, LocalDate fromDate, LocalDate toDate) {}
