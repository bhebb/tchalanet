package com.tchalanet.server.core.terminal.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import jakarta.validation.constraints.NotNull;

public record LockTerminalCommand(
    @NotNull TenantId tenantId,
    @NotNull TerminalId terminalId,
    @NotNull String reason,
    @NotNull UserId performedBy
) implements Command<Void> {}
