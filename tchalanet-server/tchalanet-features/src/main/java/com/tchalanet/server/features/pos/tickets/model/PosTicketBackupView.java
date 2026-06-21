package com.tchalanet.server.features.pos.tickets.model;

import com.tchalanet.server.core.sales.api.model.sale.TicketBackupInfo;

/**
 * Feature-level projection of {@link TicketBackupInfo}. Exposes only the fields needed by the POS
 * (displayCode, verificationShortUrl, shareableText) and never leaks the full core record.
 */
public record PosTicketBackupView(
    String displayCode,
    String verificationShortUrl,
    String shareableText
) {
    public static PosTicketBackupView from(TicketBackupInfo backup) {
        if (backup == null) {
            return null;
        }
        return new PosTicketBackupView(
            backup.displayCode(),
            backup.verificationShortUrl(),
            backup.shareableText()
        );
    }
}
