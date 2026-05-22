package com.tchalanet.server.features.cashier.games;

public record CashierBetOptionResponse(
    short code,
    String label,
    String description,
    String selectionHint
) {
}
