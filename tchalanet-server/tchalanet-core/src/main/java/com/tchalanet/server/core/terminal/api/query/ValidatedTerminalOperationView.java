package com.tchalanet.server.core.terminal.api.query;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalCapability;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalKind;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalStatus;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalSurface;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalSyncState;
import com.tchalanet.server.core.terminal.internal.domain.model.binding.TerminalBindingStatus;
import java.util.Set;

public record ValidatedTerminalOperationView(
    TerminalId terminalId,
    OutletId outletId,
    UserId assignedUserId,
    String displayCode,
    TerminalKind kind,
    TerminalSurface surface,
    TerminalStatus status,
    TerminalSyncState syncState,
    Set<TerminalCapability> capabilities,
    TerminalBindingStatus bindingStatus,
    boolean locked,
    boolean salesBlocked,
    boolean payoutBlocked,
    boolean offlineBlocked
) {}
