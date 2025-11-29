package com.tchalanet.server.core.draw.application.query.model;

import java.util.UUID;

public record ListLastDaysDrawResultsQuery(UUID tenantId, String channelCode, int days) {}
