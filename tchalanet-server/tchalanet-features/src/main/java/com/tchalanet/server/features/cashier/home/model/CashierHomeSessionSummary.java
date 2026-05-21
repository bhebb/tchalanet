package com.tchalanet.server.features.cashier.home.model;

import java.time.Instant;

public record CashierHomeSessionSummary(
    boolean open,
    Instant openedAt,
    String openedAtLabel,
    int ticketCount,
    String salesTotal) {}
