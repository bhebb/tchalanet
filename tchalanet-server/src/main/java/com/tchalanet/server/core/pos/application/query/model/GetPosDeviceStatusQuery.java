package com.tchalanet.server.core.pos.application.query.model;

import com.tchalanet.server.common.bus.Query;
import java.util.Map;
import java.util.UUID;

public record GetPosDeviceStatusQuery(UUID tenantId, UUID deviceId) implements Query<Map<String, Object>> {}
