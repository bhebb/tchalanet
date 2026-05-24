package com.tchalanet.server.catalog.plan.api;

import java.util.Set;

public final class PlanLimitKeys {

    private PlanLimitKeys() {}

    public static final String USERS_MAX = "limits.users.max";
    public static final String OUTLETS_MAX = "limits.outlets.max";
    public static final String TERMINALS_MAX = "limits.terminals.max";
    public static final String MOBILE_DEVICES_MAX = "limits.mobile_devices.max";
    public static final String PROMOTION_RULES_MAX = "limits.promotion_rules.max";
    public static final String OFFLINE_DAYS_MAX = "limits.offline_days.max";
    public static final String OFFLINE_TICKETS_PER_DEVICE_MAX = "limits.offline_tickets_per_device.max";
    public static final String EXPORTS_ROWS_MAX = "limits.exports.rows.max";

    public static final Set<String> ALL = Set.of(
        USERS_MAX,
        OUTLETS_MAX,
        TERMINALS_MAX,
        MOBILE_DEVICES_MAX,
        PROMOTION_RULES_MAX,
        OFFLINE_DAYS_MAX,
        OFFLINE_TICKETS_PER_DEVICE_MAX,
        EXPORTS_ROWS_MAX
    );
}
