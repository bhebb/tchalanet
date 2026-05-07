package com.tchalanet.server.core.terminal.application.query.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.terminal.domain.model.TerminalKind;
import com.tchalanet.server.core.terminal.domain.model.TerminalState;
import com.tchalanet.server.core.terminal.domain.model.TerminalSyncState;

/** All fields nullable = no filter. */
public record TerminalSearchCriteria(
    String q,
    OutletId outletId,
    UserId assignedUserId,
    TerminalKind kind,
    TerminalState state,
    TerminalSyncState syncState,
    Boolean activeForUser) {

  public static TerminalSearchCriteria empty() {
    return new TerminalSearchCriteria(null, null, null, null, null, null, null);
  }
}
