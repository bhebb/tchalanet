package com.tchalanet.server.core.terminal.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;

public record AssignTerminalToUserCommand(
    TenantId tenantId, TerminalId terminalId, UserId userId, UserId actorUserId)
    implements Command<Void> {}
