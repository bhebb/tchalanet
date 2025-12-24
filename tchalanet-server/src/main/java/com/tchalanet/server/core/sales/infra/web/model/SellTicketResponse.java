package com.tchalanet.server.core.sales.infra.web.model;

import java.util.List;
import java.util.UUID;

public record SellTicketResponse(
    TicketResponse ticket,
    String status,
    List<LimitNotice> warnings,
    UUID approvalRequestId
) {}

