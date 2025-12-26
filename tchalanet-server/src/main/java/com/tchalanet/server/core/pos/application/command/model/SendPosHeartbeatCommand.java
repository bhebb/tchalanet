package com.tchalanet.server.core.pos.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import java.time.Instant;
import java.util.Map;

public record SendPosHeartbeatCommand(
    TenantId tenantId,
    TerminalId deviceId,
    Instant lastSeenAt,
    String status,
    Integer batteryPercent,
    String appVersion,
    Map<String, Object> extras)
    implements Command<Void> {}
