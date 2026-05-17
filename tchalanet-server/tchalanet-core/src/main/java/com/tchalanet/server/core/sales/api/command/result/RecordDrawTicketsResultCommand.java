package com.tchalanet.server.core.sales.api.command.result;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.types.id.TenantId;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record RecordDrawTicketsResultCommand(
    @NotNull TenantId tenantId,
    @NotNull DrawId drawId,
    @NotNull DrawResultId drawResultId,
    @NotNull LocalDate drawDate,
    @NotNull ResultSlotId resultSlotId,
    @NotNull DrawChannelId drawChannelId
) implements Command<RecordDrawTicketsResultResult> {
}
