package com.tchalanet.server.core.outlet.api.query;

import java.time.LocalDate;

public record OutletDailySummary(
    LocalDate date,
    long totalTickets,
    long sold,
    long voided,
    long resultedWin,
    long resultedLoss,
    long paid,
    int sessionCount,
    String outletName,
    boolean salesBlocked) {}
