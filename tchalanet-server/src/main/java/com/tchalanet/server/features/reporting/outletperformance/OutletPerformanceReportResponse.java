package com.tchalanet.server.features.reporting.outletperformance;

import java.time.LocalDate;
import java.util.List;

public record OutletPerformanceReportResponse(
    LocalDate fromDate,
    LocalDate toDate,
    String gameCode,
    List<OutletPerformanceLineDto> outlets) {}
