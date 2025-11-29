package com.tchalanet.server.draw.application.query.model;

import java.util.UUID;

public record ListLastDaysDrawsQuery(UUID tenantId, String channelCode, int days) {}
