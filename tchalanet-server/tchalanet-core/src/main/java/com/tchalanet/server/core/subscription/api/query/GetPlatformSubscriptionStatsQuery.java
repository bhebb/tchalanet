package com.tchalanet.server.core.subscription.api.query;

import com.tchalanet.server.common.bus.Query;

/**
 * Platform-level aggregated subscription stats (cross-tenant read).
 * SUPER_ADMIN only via platform scope (RLS allows select cross-tenant).
 */
public record GetPlatformSubscriptionStatsQuery() implements Query<PlatformSubscriptionStatsView> {}

