package com.tchalanet.server.draw.application.query.model;

import java.util.UUID;

public record DrawQuery(UUID tenantId, UUID drawId) {}
