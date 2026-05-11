package com.tchalanet.server.core.limitpolicy.application.command.model.exposure;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.core.sales.domain.event.TicketPlacedEvent;
import jakarta.validation.constraints.NotNull;

public record ApplyTicketExposureCommand(
    @NotNull TicketPlacedEvent event
) implements Command<Void> {}
