package com.tchalanet.server.core.outlet.api.query;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;

public record OutletTerminalView(
    TerminalId terminalId,
    OutletId outletId,
    String label,
    String kind,
    String state,
    String syncState,
    UserId assignedUserId,
    boolean activeForUser,
    Instant lastSeen) {}
