package com.tchalanet.server.core.tenant.application.query.model;

import com.tchalanet.server.common.bus.Query;

/** Query to retrieve global tenant statistics (counts by status). */
public record GetTenantGlobalStatsQuery() implements Query<TenantGlobalStatsView> {}
