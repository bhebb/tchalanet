package com.tchalanet.server.core.terminal.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalKind;
import java.util.Map;

public record RegisterTerminalCommand(
    TenantId tenantId,
    OutletId outletId,
    TerminalKind kind,
    String label,
    String inventoryTag,
    Map<String, Object> metadata,
    UserId actorUserId)
    implements Command<TerminalId> {}
