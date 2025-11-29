package com.tchalanet.server.core.draw.application.command.model;

import java.time.LocalDate;
import java.util.UUID;

public record GenerateDrawsForDateRangeCommand(
    UUID tenantId, LocalDate fromDate, LocalDate toDate, int chunkSize) {}
