package com.tchalanet.server.features.pos.tickets.model;

import java.util.List;

public record SellerTerminalDailyStatsResponse(
    long ticketCount,
    long salesTotalCents,
    String currency,
    List<DrawStatLineDto> breakdown) {}
