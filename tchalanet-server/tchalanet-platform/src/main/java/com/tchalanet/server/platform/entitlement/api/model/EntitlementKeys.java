package com.tchalanet.server.platform.entitlement.api.model;

public final class EntitlementKeys {
    private EntitlementKeys() {}

    // Features
    public static final String FEATURE_SALE_MANUAL = "sale.manual";
    public static final String FEATURE_SALE_OFFLINE = "sale.offline";
    public static final String FEATURE_PROMOTION_BASIC = "promotion.basic";
    public static final String FEATURE_PAYOUT_AUTO_APPROVE = "payout.auto_approve";

    // Limits
    public static final String LIMIT_OUTLETS = "outlets";
    public static final String LIMIT_TERMINALS = "terminals";
    public static final String LIMIT_USERS = "users";
    public static final String LIMIT_MAX_TICKETS_PER_DAY = "maxTicketsPerDay";
}
