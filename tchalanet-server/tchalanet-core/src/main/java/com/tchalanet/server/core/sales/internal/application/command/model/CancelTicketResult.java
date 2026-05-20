package com.tchalanet.server.core.sales.internal.application.command.model;

import com.tchalanet.server.common.web.api.ApiNotice;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.Ticket;

import java.util.List;

public record CancelTicketResult(
    Ticket ticket,
    CancelOutcome outcome,
    List<ApiNotice> warnings
) {
    public enum CancelOutcome {
        SUCCESS,
        SUCCESS_WITH_WARNINGS
    }
}

