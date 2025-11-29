package com.tchalanet.server.core.draw.application.query.model;

import java.util.UUID;

public record ListDrawChannelsQuery(UUID tenantId, Boolean activeOnly) {}
