package com.tchalanet.server.core.sales.api.model.receipt;

import java.math.BigDecimal;
import java.util.Objects;

public record TicketReceiptLineView(
    int lineNo,
    String gameCode,
    String betType,
    Short betOption,
    String gameLabel,
    String selection,
    BigDecimal odds,
    String stake,
    String potentialPayout
) {
    public TicketReceiptLineView {
        Objects.requireNonNull(gameCode, "gameCode is required");
        Objects.requireNonNull(betType, "betType is required");
        Objects.requireNonNull(selection, "selection is required");
        Objects.requireNonNull(stake, "stake is required");
        Objects.requireNonNull(potentialPayout, "potentialPayout is required");
    }
}
