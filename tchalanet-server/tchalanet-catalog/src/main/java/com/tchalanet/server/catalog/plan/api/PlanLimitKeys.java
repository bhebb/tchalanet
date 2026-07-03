package com.tchalanet.server.catalog.plan.api;

import java.util.Set;

public final class PlanLimitKeys {

    private PlanLimitKeys() {}

    public static final String ADMIN_USERS_MAX = "limits.admin_users.max";
    public static final String SELLER_TERMINALS_MAX = "limits.seller_terminals.max";
    public static final String DRAW_CHANNELS_MAX = "limits.draw_channels.max";
    public static final String PROMOTION_RULES_MAX = "limits.promotion_rules.max";

    public static final Set<String> ALL = Set.of(
        ADMIN_USERS_MAX,
        SELLER_TERMINALS_MAX,
        DRAW_CHANNELS_MAX,
        PROMOTION_RULES_MAX
    );
}
