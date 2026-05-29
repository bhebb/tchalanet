package com.tchalanet.server.features.cashier.tickets.model;

public enum CashierTicketVerificationStatus {
    NOT_FOUND,
    NOT_PAYABLE_PENDING_DRAW,
    NOT_PAYABLE_RESULT_PENDING,
    NOT_PAYABLE_LOST,
    PAYABLE,
    ALREADY_PAID,
    BLOCKED,
    CANCELLED,
    VOIDED,
    REPAIR_REQUIRED,
    OPERATION_NOT_ALLOWED
}
