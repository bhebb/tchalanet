package com.tchalanet.server.draw.application.query.model;

import java.util.UUID;

public record GetDrawResultQuery(UUID tenantId, UUID drawId) {}
