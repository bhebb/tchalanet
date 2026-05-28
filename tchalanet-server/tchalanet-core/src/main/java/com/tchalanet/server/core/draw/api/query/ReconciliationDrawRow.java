package com.tchalanet.server.core.draw.api.query;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.api.model.DrawStatus;
import java.time.Instant;
import java.time.LocalDate;

public record ReconciliationDrawRow(
    TenantId tenantId,
    DrawId drawId,
    DrawChannelId drawChannelId,
    DrawResultId drawResultId,
    LocalDate drawDate,
    Instant scheduledAt,
    Instant resultedAt,
    DrawStatus status
) {}
