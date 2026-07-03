package com.tchalanet.server.catalog.plan.api;

import java.util.Set;

public final class PlanFeatureKeys {

    private PlanFeatureKeys() {}

    public static final String THEME_PRESET_SELECTION = "theme.preset_selection";
    public static final String TENANTGAME_MANAGEMENT = "tenantgame.management";
    public static final String PROMOTION_CAMPAIGN_ADMIN = "promotion.campaigns.config";

    public static final Set<String> ALL = Set.of(
        THEME_PRESET_SELECTION,
        TENANTGAME_MANAGEMENT,
        PROMOTION_CAMPAIGN_ADMIN
    );
}
