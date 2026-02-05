package com.tchalanet.server.catalog.billing.application.query.model;

import com.tchalanet.server.common.bus.Query;

/** Query to retrieve global subscription statistics (counts by status and plan). */
public record GetSubscriptionGlobalStatsQuery()
    implements Query<SubscriptionGlobalStatsView> {}
