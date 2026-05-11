package com.tchalanet.server.core.sales.application.command.model;

import com.tchalanet.server.core.sales.domain.model.Ticket;

public record MarkTicketPayoutPaidResult(Ticket ticket, boolean updated) {}

