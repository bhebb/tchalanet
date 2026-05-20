package com.tchalanet.server.core.sales.api.model.receipt;

import java.util.List;
import java.util.Objects;

public record TicketReceiptGameSectionView(
    String gameCode,
    String gameLabel,
    List<TicketReceiptLineView> lines
) {
    public TicketReceiptGameSectionView {
        Objects.requireNonNull(gameCode, "gameCode is required");
        lines = List.copyOf(lines);
    }
}
