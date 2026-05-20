package com.tchalanet.server.core.terminal.api.query;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.terminal.internal.domain.model.Terminal;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalKind;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalState;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalSyncState;

import java.time.Instant;

public record TerminalSummaryView(
    TerminalId id,
    OutletId outletId,
    UserId assignedUserId,
    TerminalKind kind,
    TerminalState state,
    TerminalSyncState syncState,
    boolean autoSessionEnabled,
    Instant lastSeen,
    String label,
    String inventoryTag,
    String code,
    boolean locked,
    boolean salesBlocked,
    boolean payoutBlocked,
    boolean offlineBlocked) {

    public static TerminalSummaryView from(Terminal t) {
        return new TerminalSummaryView(
            t.id(),
            t.outletId(),
            t.assignedUserId(),
            t.kind(),
            t.state(),
            t.syncState(),
            t.autoSessionEnabled(),
            t.lastSeen(),
            t.label(),
            t.inventoryTag(),
            t.code(),
            t.locked(),
            t.salesBlocked(),
            t.payoutBlocked(),
            t.offlineBlocked());
    }
}

