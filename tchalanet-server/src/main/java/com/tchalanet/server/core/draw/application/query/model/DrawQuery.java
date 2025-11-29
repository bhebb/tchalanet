package com.tchalanet.server.core.draw.application.query.model;

import java.util.UUID;

public record DrawQuery(UUID tenantId, UUID drawId) {}
