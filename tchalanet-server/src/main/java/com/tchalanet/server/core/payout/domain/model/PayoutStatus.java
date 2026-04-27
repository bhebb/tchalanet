package com.tchalanet.server.core.payout.domain.model;

/** Status of a payout. */
public enum PayoutStatus {
    REQUESTED,
    APPROVED,
    PARTIALLY_PAID,
    PAID,
    REJECTED
}
