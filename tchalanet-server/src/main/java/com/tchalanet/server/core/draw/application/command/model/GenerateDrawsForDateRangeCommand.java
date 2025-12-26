package com.tchalanet.server.core.draw.application.command.model;
import com.tchalanet.server.common.types.id.TenantId;

import java.time.LocalDate;
import java.util.UUID;

public record GenerateDrawsForDateRangeCommand(
    TenantId tenantId, LocalDate fromDate, LocalDate toDate, int chunkSize) {}
