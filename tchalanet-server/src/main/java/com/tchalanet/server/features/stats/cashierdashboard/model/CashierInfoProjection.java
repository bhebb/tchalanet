package com.tchalanet.server.features.stats.cashierdashboard.model;

import java.util.UUID;

public record CashierInfoProjection(
    UUID cashierId, String cashierName, UUID outletId, String outletName) {}
