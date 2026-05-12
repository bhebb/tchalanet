package com.tchalanet.server.core.terminal.internal.infra.web.tenant.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.terminal.domain.model.TerminalKind;
import com.tchalanet.server.core.terminal.domain.model.TerminalState;
import com.tchalanet.server.core.terminal.domain.model.TerminalSyncState;

import java.time.Instant;
import java.util.Map;

public record TerminalResponse(
    TerminalId id,
    OutletId outletId,
    UserId assignedUserId,
    TerminalKind kind,
    String label,
    String inventoryTag,
    TerminalState state,
    TerminalSyncState syncState,
    boolean activeForUser,
    Instant lastSeenAt,
    Map<String, Object> metadata) {
}
