package com.tchalanet.server.features.pos.tickets.model;

public record DrawStatLineItem(
    String drawId, String channelLabel, long ticketCount, long totalCents) {}
