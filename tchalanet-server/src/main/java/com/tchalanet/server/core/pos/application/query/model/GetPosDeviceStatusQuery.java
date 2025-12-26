package com.tchalanet.server.core.pos.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.core.pos.domain.model.Terminal;
import java.util.Map;

public record GetPosDeviceStatusQuery(TenantId tenantId, TerminalId deviceId) implements Query<Map<String, Object>> {}
