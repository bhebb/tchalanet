package com.tchalanet.server.draw.application.query.model;

import java.util.UUID;

public record ListTodayDrawsQuery(UUID tenantId, String channelCode) {}
