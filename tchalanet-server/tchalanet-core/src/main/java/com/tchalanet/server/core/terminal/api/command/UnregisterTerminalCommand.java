package com.tchalanet.server.core.terminal.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;

public record UnregisterTerminalCommand(
    TenantId tenantId, TerminalId terminalId, String reason, UserId actorUserId)
    implements Command<Void> {}
