package com.tchalanet.server.core.draw.application.command.model;

import com.tchalanet.server.common.types.id.TenantId;
import java.time.LocalDate;

public record GenerateDrawsForDateRangeCommand(
    TenantId tenantId, LocalDate fromDate, LocalDate toDate, int chunkSize) {}
