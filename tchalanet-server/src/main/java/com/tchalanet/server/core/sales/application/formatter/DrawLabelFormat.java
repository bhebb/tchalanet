package com.tchalanet.server.core.sales.application.formatter;

public enum DrawLabelFormat {
    TICKET_SHORT,     // reçu thermique
    TICKET_FULL,      // PDF / détail complet
    SMS_SHORT,        // SMS / WhatsApp compact
    ADMIN_DISPLAY,    // backoffice
    ISO_DEBUG         // logs/debug/support
}
