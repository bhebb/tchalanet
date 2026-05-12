package com.tchalanet.server.core.sales.api.command;

import com.tchalanet.server.core.sales.domain.model.Ticket;

public record TicketRejectedResult(Ticket ticket) {}

