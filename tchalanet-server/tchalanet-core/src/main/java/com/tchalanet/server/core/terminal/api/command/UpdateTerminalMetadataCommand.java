package com.tchalanet.server.core.terminal.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import java.util.Map;

public record UpdateTerminalMetadataCommand(
    TenantId tenantId,
    TerminalId terminalId,
    Map<String, Object> metadataPatch,
    UserId actorUserId)
    implements Command<Void> {}
