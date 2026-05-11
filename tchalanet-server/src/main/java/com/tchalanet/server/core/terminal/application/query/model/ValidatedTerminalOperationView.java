package com.tchalanet.server.core.terminal.application.query.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.terminal.domain.model.TerminalState;

public record ValidatedTerminalOperationView(
    TerminalId terminalId,
    OutletId outletId,
    UserId assignedUserId,
    String displayCode,
    TerminalState state,
    boolean locked,
    boolean salesBlocked,
    boolean payoutBlocked,
    boolean offlineBlocked
) {}
