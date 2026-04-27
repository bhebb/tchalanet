package com.tchalanet.server.core.sales.application.command.model;

import com.tchalanet.server.core.sales.domain.model.Ticket;
import java.util.UUID;

public record SellTicketResult(
    Ticket ticket,
    SellTicketOutcome outcome,
    UUID approvalRequestId
) {}


