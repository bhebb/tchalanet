package com.tchalanet.server.core.pos.application.query.model;

import java.util.UUID;

public record GetPosDeviceByIdQuery(UUID tenantId, UUID deviceId) {}

