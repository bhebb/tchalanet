package com.tchalanet.server.core.sales.internal.infra.web.model;

import com.tchalanet.server.common.types.id.ApprovalRequestId;
import com.tchalanet.server.core.sales.api.command.SellTicketOutcome;
import java.util.List;

public record SellTicketResponse(
    TicketResponse ticket,
    SellTicketOutcome status,
    List<LimitNotice> warnings,
    ApprovalRequestId approvalRequestId) {
}
