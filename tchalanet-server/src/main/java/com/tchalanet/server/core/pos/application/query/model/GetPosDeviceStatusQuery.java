package com.tchalanet.server.core.pos.application.query.model;

import java.util.UUID;

public record GetPosDeviceStatusQuery(UUID tenantId, UUID deviceId) {}

