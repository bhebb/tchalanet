package com.tchalanet.server.features.reporting.salesreport;

import java.time.LocalDate;
import java.util.List;

public record SalesReportResponse(
    LocalDate fromDate, LocalDate toDate, String gameCode, List<SalesReportLineDto> lines) {}
