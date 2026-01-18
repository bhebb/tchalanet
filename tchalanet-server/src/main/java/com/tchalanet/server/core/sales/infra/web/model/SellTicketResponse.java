package com.tchalanet.server.core.sales.infra.web.model;

import com.tchalanet.server.core.sales.application.command.model.SellTicketOutcome;

import java.util.List;
import java.util.UUID;

public record SellTicketResponse(
    TicketResponse ticket, SellTicketOutcome status, List<LimitNotice> warnings, UUID approvalRequestId) {
}
