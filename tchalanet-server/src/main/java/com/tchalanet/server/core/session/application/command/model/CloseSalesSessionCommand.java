package com.tchalanet.server.core.session.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import jakarta.validation.constraints.NotNull;

public record CloseSalesSessionCommand(
    @NotNull TenantId tenantId,
    @NotNull SalesSessionId sessionId,
    long declaredClosingAmountCents,
    @NotNull UserId closedBy,
    String reason) implements Command<CloseSalesSessionResult> {
}
