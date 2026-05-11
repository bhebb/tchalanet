package com.tchalanet.server.core.offlinesync.application.command.model.code;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;

public record IssueOfflineCodeBatchCommand(
    TenantId tenantId,
    TerminalId terminalId,
    UserId issuedBy,
    int requestedCount,
    Instant expiresAt
) implements Command<IssueOfflineCodeBatchResult> {}

