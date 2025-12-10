package com.tchalanet.server.core.pos.application.query.model;

import java.util.UUID;

public record ListPosDevicesByLocationQuery(UUID tenantId, UUID outletId) {}

