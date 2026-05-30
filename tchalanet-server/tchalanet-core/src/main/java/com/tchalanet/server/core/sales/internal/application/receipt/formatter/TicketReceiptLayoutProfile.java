package com.tchalanet.server.core.sales.internal.application.receipt.formatter;

import com.tchalanet.server.platform.document.api.model.DocumentPrintProfile;

public record TicketReceiptLayoutProfile(
    int charsPerLine,
    boolean compact,
    boolean compactCurrencyDisplay,
    boolean printFullVerificationUrl
) {
    public TicketReceiptLayoutProfile {
        if (charsPerLine <= 0) {
            throw new IllegalArgumentException("charsPerLine must be positive");
        }
    }

    public static TicketReceiptLayoutProfile from(DocumentPrintProfile profile) {
        boolean receipt = profile.receiptPaper();

        return new TicketReceiptLayoutProfile(
            profile.textColumns(),
            profile.compact(),
            receipt,
            !receipt
        );
    }
}

