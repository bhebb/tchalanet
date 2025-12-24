package com.tchalanet.server.core.pos.application.command.model;

import com.tchalanet.server.common.bus.Command;
import java.util.UUID;
import java.time.Instant;
import java.util.Map;

public record SendPosHeartbeatCommand(
    UUID tenantId,
    UUID deviceId,
    Instant lastSeenAt,
    String status,
    Integer batteryPercent,
    String appVersion,
    Map<String,Object> extras
) implements Command<Void> {}
