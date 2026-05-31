package com.tchalanet.server.core.terminal.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import jakarta.validation.constraints.NotNull;

public record SetTerminalOperationalControlCommand(
    @NotNull TenantId tenantId,
    @NotNull TerminalId terminalId,
    @NotNull TerminalOperationalControl control,
    boolean blocked,
    String reason,
    @NotNull UserId performedBy
) implements Command<Void> {
}
