package com.tchalanet.server.core.terminal.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.core.terminal.domain.model.Terminal;
import java.util.UUID;

public record LockTerminalCommand(
    TenantId tenantId, TerminalId terminalId, UUID actorId, String reason)
    implements Command<Terminal> {}
