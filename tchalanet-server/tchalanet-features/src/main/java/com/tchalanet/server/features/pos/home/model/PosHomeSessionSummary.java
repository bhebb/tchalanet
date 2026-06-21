package com.tchalanet.server.features.pos.home.model;

import java.time.Instant;

public record PosHomeSessionSummary(
    boolean open,
    Instant openedAt,
    String openedAtLabel,
    int ticketCount,
    String salesTotal) {}
