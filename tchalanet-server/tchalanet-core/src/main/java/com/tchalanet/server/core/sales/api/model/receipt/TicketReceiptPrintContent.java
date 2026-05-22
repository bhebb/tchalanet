package com.tchalanet.server.core.sales.api.model.receipt;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public record TicketReceiptPrintContent(
    String title,
    List<String> bodyLines,
    String qrPayload,
    String filenameBase,
    Locale locale,
    Map<String, String> metadata
) {
    public TicketReceiptPrintContent {
        Objects.requireNonNull(title, "title is required");
        bodyLines = List.copyOf(bodyLines);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
