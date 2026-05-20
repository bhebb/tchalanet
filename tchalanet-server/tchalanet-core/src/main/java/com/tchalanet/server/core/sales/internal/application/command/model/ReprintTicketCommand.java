package com.tchalanet.server.core.sales.internal.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;
import jakarta.validation.constraints.NotNull;

public record ReprintTicketCommand(
    @NotNull TenantId tenantId,
    @NotNull TicketId ticketId,
    @NotNull UserId requestedBy
) implements Command<Void> {}
