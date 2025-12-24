package com.tchalanet.server.core.pos.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.core.pos.domain.model.Terminal;
import java.util.UUID;

public record UnregisterTerminalCommand(UUID tenantId, UUID terminalId, UUID actorId, String reason) implements Command<Terminal> {}
