package com.tchalanet.server.draw.application.query.model;

import java.util.UUID;

public record ListTodayDrawResultQuery(UUID tenantId, String channelCode) {}
