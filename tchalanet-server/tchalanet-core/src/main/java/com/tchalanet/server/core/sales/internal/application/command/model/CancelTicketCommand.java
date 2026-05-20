package com.tchalanet.server.core.sales.internal.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CancelTicketCommand(
    @NotNull TenantId tenantId,
    @NotNull TicketId ticketId,
    @NotNull UserId cancelledBy,
    @NotBlank(message = "Cancellation reason is required")
    @Size(min = 3, max = 500, message = "Reason must be between 3 and 500 characters")
    String reason
) implements Command<CancelTicketResult> {}
