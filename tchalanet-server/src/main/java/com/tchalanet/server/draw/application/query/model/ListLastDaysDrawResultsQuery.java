package com.tchalanet.server.draw.application.query.model;

import java.util.UUID;

public record ListLastDaysDrawResultsQuery(UUID tenantId, String channelCode, int days) {}
