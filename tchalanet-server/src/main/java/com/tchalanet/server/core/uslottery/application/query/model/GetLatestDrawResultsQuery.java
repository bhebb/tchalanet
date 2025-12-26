package com.tchalanet.server.core.uslottery.application.query.model;

import com.tchalanet.server.common.types.id.TenantId;

/** Query used by the US-lottery domain to request recent draw results for a channel. */
public record GetLatestDrawResultsQuery(TenantId tenantId, String channelCode, int days) {}
