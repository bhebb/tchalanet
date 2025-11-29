package com.tchalanet.server.draw.application.query.model;

import java.util.UUID;

public record GetDrawQuery(UUID tenantId, UUID drawId) {}
