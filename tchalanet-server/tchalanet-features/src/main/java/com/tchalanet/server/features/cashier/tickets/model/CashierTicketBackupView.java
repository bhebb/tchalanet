package com.tchalanet.server.features.cashier.tickets.model;

import com.tchalanet.server.core.sales.api.model.sale.TicketBackupInfo;

/**
 * Feature-level projection of {@link TicketBackupInfo}. Exposes only the fields needed by the POS
 * (displayCode, verificationShortUrl, shareableText) and never leaks the full core record.
 */
public record CashierTicketBackupView(
    String displayCode,
    String verificationShortUrl,
    String shareableText
) {
    public static CashierTicketBackupView from(TicketBackupInfo backup) {
        if (backup == null) {
            return null;
        }
        return new CashierTicketBackupView(
            backup.displayCode(),
            backup.verificationShortUrl(),
            backup.shareableText()
        );
    }
}
