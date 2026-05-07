package com.tchalanet.server.core.terminal.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;

public record UnlockTerminalCommand(
    TenantId tenantId, TerminalId terminalId, UserId actorUserId) implements Command<Void> {}
