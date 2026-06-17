package com.tchalanet.server.features.cashier.tickets.model;

import java.util.List;

public record TerminalDailyStatsResponse(
    long ticketCount,
    long salesTotalCents,
    String currency,
    List<DrawStatLineDto> breakdown) {}
