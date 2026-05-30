package com.tchalanet.server.core.draw.api.query;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;
import java.time.LocalDate;

public record OpenableDrawRow(
    TenantId tenantId,
    DrawId drawId,
    ResultSlotId resultSlotId,
    LocalDate drawDate,
    boolean locked,
    Instant scheduledAt,
    Instant cutoffAt) {}
