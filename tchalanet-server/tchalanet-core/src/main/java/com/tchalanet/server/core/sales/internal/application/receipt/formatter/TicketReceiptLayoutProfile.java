package com.tchalanet.server.core.sales.internal.application.receipt.formatter;

import com.tchalanet.server.platform.document.api.model.DocumentPrintProfile;
import com.tchalanet.server.platform.document.api.model.PaperSize;

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
        // A4 uses non-compact spacing (readable page layout);
        // thermal (80mm/58mm) uses compact spacing (narrow strip).
        // Both always use a single "Devise : <code>" currency note and never
        // print the full verification URL as text — the QR carries it.
        boolean isA4 = profile.paperSize() == PaperSize.A4;
        return new TicketReceiptLayoutProfile(
            profile.textColumns(),
            !isA4,   // compact=false for A4, true for thermal
            true,    // always single currency note, no per-line HTG
            false    // never print full URL as text
        );
    }
}

