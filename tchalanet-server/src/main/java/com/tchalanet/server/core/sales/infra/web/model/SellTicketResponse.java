package com.tchalanet.server.core.sales.infra.web.model;

import com.tchalanet.server.common.types.id.ApprovalRequestId;
import com.tchalanet.server.core.sales.application.command.model.SellTicketOutcome;
import java.util.List;

public record SellTicketResponse(
    TicketResponse ticket,
    SellTicketOutcome status,
    List<LimitNotice> warnings,
    ApprovalRequestId approvalRequestId) {
}
