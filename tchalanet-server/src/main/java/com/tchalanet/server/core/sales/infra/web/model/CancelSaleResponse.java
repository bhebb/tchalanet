package com.tchalanet.server.core.sales.infra.web.model;

import java.util.List;

public record CancelSaleResponse(
    TicketResponse ticket, String status, List<LimitNotice> warnings) {}
