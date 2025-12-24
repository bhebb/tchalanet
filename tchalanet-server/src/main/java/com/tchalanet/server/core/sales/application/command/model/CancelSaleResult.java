package com.tchalanet.server.core.sales.application.command.model;

import com.tchalanet.server.core.sales.domain.model.Ticket;

import java.util.List;

public record CancelSaleResult(
    Ticket ticket,
    String status,
    List<LimitNotice> warnings
) {}
