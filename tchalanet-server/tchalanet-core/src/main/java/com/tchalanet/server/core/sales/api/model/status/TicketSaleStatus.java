package com.tchalanet.server.core.sales.api.model.status;

public enum TicketSaleStatus {
    PENDING_APPROVAL,
    REJECTED,
    APPROVED,
    CANCELLED,
    VOIDED;

    public boolean isAcceptedSale() {
        return this == APPROVED;
    }

    public boolean isPrintable() {
        return this == APPROVED || this == CANCELLED;
    }

    public boolean isCancellable() {
        return this == PENDING_APPROVAL || this == APPROVED;
    }
    public boolean isFinal() {
        return this == REJECTED || this == CANCELLED || this == VOIDED;
    }
}
