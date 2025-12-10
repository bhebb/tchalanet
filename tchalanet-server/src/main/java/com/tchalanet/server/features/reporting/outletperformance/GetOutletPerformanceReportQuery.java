package com.tchalanet.server.features.reporting.outletperformance;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Requête pour le rapport de performance des outlets.
 */
public record GetOutletPerformanceReportQuery(
    UUID tenantId,
    LocalDate fromDate,
    LocalDate toDate,
    String gameCode // optionnel, peut être null
) {
}
