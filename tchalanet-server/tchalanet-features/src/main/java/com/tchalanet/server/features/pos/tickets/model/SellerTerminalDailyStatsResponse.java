package com.tchalanet.server.features.pos.tickets.model;

import java.util.List;

public record SellerTerminalDailyStatsResponse(
    long ticketCount,
    long salesTotalCents,
    long sellerCommissionTotalCents,
    String currency,
    List<DrawStatLineItem> breakdown) {}
