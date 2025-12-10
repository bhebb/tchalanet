package com.tchalanet.server.core.uslottery.application.query.model;

import java.util.UUID;

/** Query used by the US-lottery domain to request recent draw results for a channel. */
public record GetLatestDrawResultsQuery(UUID tenantId, String channelCode, int days) {}

