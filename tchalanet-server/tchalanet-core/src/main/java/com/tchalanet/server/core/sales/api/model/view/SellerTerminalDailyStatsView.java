package com.tchalanet.server.core.sales.api.model.view;

import java.util.List;

public record SellerTerminalDailyStatsView(
    long ticketCount,
    long salesTotalCents,
    String currency,
    List<DrawStatLine> breakdown) {}
