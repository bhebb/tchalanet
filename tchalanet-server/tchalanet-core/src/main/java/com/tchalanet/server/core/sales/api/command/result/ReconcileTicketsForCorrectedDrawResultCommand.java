package com.tchalanet.server.core.sales.api.command.result;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.TenantId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReconcileTicketsForCorrectedDrawResultCommand(
    @NotNull TenantId tenantId,
    @NotNull DrawId drawId,
    @NotNull DrawResultId previousDrawResultId,
    @NotNull DrawResultId correctedDrawResultId,
    @NotBlank String reason
) implements Command<ReconcileTicketsForCorrectedDrawResultResult> {
}
