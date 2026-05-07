package com.tchalanet.server.core.terminal.application.query.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.terminal.domain.model.Terminal;
import com.tchalanet.server.core.terminal.domain.model.TerminalKind;
import com.tchalanet.server.core.terminal.domain.model.TerminalState;
import com.tchalanet.server.core.terminal.domain.model.TerminalSyncState;
import java.time.Instant;

public record TerminalSummaryView(
    TerminalId id,
    OutletId outletId,
    UserId assignedUserId,
    TerminalKind kind,
    TerminalState state,
    boolean activeForUser,
    TerminalSyncState syncState,
    Instant lastSeen,
    String label) {

  public static TerminalSummaryView from(Terminal t) {
    return new TerminalSummaryView(
        t.id(),
        t.outletId(),
        t.assignedUserId(),
        t.kind(),
        t.state(),
        t.activeForUser(),
        t.syncState(),
        t.lastSeen(),
        t.label());
  }
}
