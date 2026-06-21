package com.tchalanet.server.features.pos.tickets.model;

public record DrawStatLineDto(String drawId, String channelLabel, long ticketCount, long totalCents) {}
