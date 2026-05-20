package com.tchalanet.server.core.terminal.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalSyncState;

public record UpdateTerminalSyncStateCommand(
    TenantId tenantId, TerminalId terminalId, TerminalSyncState newSyncState, UserId actorUserId)
    implements Command<Void> {}
