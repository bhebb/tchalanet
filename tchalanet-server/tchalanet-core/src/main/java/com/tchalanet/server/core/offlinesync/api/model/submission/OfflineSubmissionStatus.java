package com.tchalanet.server.core.offlinesync.api.model.submission;

public enum OfflineSubmissionStatus {
    RECEIVED,
    TECH_VALIDATED,
    TECH_REJECTED,
    PROMOTION_REQUESTED,
    PROMOTED,
    BUSINESS_REJECTED,
    NEEDS_ADMIN_REVIEW,
    ADMIN_APPROVED,
    ADMIN_REJECTED,
    SYNC_FAILED
}
