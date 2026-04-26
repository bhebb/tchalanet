package com.tchalanet.server.core.sales.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;

/**
 * Batch settlement command triggered after DrawResult has been applied to a Draw.
 * Sales calculates ticket winnings and stores RESULTED_*.
 */
public record RecordDrawTicketsResultCommand(
    TenantId tenantId,
    DrawId drawId,
    DrawResultId drawResultId,
    Instant occurredAt
) implements Command<RecordDrawTicketsResultResult> {}
