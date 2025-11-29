package com.tchalanet.server.draw.application.query.model;

import java.util.UUID;

public record GetDrawChannelQuery(UUID tenantId, UUID channelId) {}
