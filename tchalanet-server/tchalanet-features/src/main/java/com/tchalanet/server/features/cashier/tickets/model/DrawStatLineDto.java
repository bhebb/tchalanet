package com.tchalanet.server.features.cashier.tickets.model;

public record DrawStatLineDto(String drawId, String channelLabel, long ticketCount, long totalCents) {}
