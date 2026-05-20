package com.tchalanet.server.core.sales.internal.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.Ticket;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ApproveTicketSaleCommand(
    @NotNull TenantId tenantId,
    @NotNull TicketId ticketId,
    @NotNull UserId approvedBy,
    @Size(max = 500) String reason
) implements Command<Ticket> {}
