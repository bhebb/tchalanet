package com.tchalanet.server.platform.document.internal.receipt;

import java.util.List;

public record ReceiptModel(
    String title,
    List<ReceiptLine> lines
) {

    public ReceiptModel {
        title = title == null || title.isBlank() ? "Ticket Tchalanet" : title;
        lines = lines == null ? List.of() : List.copyOf(lines);
    }
}
