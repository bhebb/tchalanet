package com.tchalanet.server.features.reporting.adminreports;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

public record AdminReportPeriodRequest(
    LocalDate from,
    LocalDate to,
    ZoneId zoneId,
    int drawLimit,
    int sellerTerminalLimit,
    List<UUID> drawIds,
    List<UUID> sellerTerminalIds) {}
