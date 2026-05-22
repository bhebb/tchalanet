package com.tchalanet.server.core.sales.api.model.sale;

import java.util.Objects;

public record TicketBackupInfo(
    String displayCode,
    String verificationShortUrl,
    String shareableText,
    String primaryInstruction,
    String verificationInstruction
) {
    public TicketBackupInfo {
        Objects.requireNonNull(displayCode, "displayCode is required");
        Objects.requireNonNull(verificationShortUrl, "verificationShortUrl is required");
        Objects.requireNonNull(shareableText, "shareableText is required");
    }
}
