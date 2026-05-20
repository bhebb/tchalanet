package com.tchalanet.server.core.limitpolicy.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.core.sales.api.event.TicketPlacedEvent;
import jakarta.validation.constraints.NotNull;

public record ApplyTicketExposureCommand(
    @NotNull TicketPlacedEvent event
) implements Command<Void> {}
