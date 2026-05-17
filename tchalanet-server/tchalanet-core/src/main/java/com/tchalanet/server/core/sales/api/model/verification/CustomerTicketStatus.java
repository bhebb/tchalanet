package com.tchalanet.server.core.sales.api.model.verification;

public enum CustomerTicketStatus {
    AWAITING_RESULT,
    LOST,
    WON_CLAIMABLE,
    WON_PAID,
    CANCELLED,
    VOIDED,
    CORRECTED,
    EXPIRED
}
