package com.tchalanet.server.core.terminal.api.query;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.terminal.internal.domain.model.Terminal;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalKind;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalState;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalSyncState;
import java.time.Instant;
import java.util.Map;

public record TerminalView(
    TerminalId id,
    TenantId tenantId,
    OutletId outletId,
    UserId assignedUserId,
    TerminalKind kind,
    TerminalState state,
    boolean activeForUser,
    TerminalSyncState syncState,
    Instant lastSeen,
    String label,
    String inventoryTag,
    Map<String, Object> metadata,
    Instant registeredAt,
    Instant unregisteredAt,
    Instant lockedAt,
    UserId lockedBy,
    String lockReason) {

  public static TerminalView from(Terminal t) {
    return new TerminalView(
        t.id(),
        t.tenantId(),
        t.outletId(),
        t.assignedUserId(),
        t.kind(),
        t.state(),
        t.autoSessionEnabled(),
        t.syncState(),
        t.lastSeen(),
        t.label(),
        t.inventoryTag(),
        t.metadata(),
        t.registeredAt(),
        t.unregisteredAt(),
        t.lockedAt(),
        t.lockedBy(),
        t.lockReason());
  }
}
