package com.tchalanet.server.core.pos.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.core.pos.domain.model.Terminal;
import java.util.UUID;

public record UnlockTerminalCommand(TenantId tenantId, TerminalId terminalId, UUID actorId)
    implements Command<Terminal> {}
