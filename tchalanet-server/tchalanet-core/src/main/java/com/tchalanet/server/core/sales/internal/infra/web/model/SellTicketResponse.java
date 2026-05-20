package com.tchalanet.server.core.sales.internal.infra.web.model;


import com.tchalanet.server.common.types.id.ApprovalRequestId;

public record SellTicketResponse(
    SellTicketResponseOutcome outcome,
    TicketResponse ticket,
    ApprovalRequestId approvalRequestId
) {}
