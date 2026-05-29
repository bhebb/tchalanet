package com.tchalanet.server.core.drawresult.api.query.reconciliation;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.core.draw.api.model.DrawStatus;
import com.tchalanet.server.core.drawresult.internal.domain.model.DrawResultStatus;
import java.time.Instant;
import java.time.LocalDate;

public record ReconciliationDrawResultRow(
    DrawId drawId,
    DrawResultId drawResultId,
    DrawChannelId drawChannelId,
    LocalDate businessDate,
    Instant scheduledAt,
    Instant resultedAt,
    DrawStatus drawStatus,
    DrawResultStatus resultStatus
) {}
