package com.tchalanet.server.core.sales.api.model.receipt;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public record TicketReceiptMessageContent(
    String subject,
    String body,
    Locale locale,
    Map<String, String> metadata
) {
    public TicketReceiptMessageContent {
        Objects.requireNonNull(subject, "subject is required");
        Objects.requireNonNull(body, "body is required");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
