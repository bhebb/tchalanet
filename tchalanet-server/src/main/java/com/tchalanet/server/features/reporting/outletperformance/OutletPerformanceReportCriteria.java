package com.tchalanet.server.features.reporting.outletperformance;

import java.time.LocalDate;
import java.util.UUID;

public record OutletPerformanceReportCriteria(
    UUID tenantId,
    LocalDate fromDate,
    LocalDate toDate,
    String gameCode // optionnel, peut être null
    ) {}
