package com.tchalanet.server.core.draw.application.query.projection;

import java.util.UUID;

public record DueToCloseRow(UUID tenantId, UUID drawId, boolean locked) {}
