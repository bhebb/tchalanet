package com.tchalanet.server.core.offlinesync.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.OfflineCodeBatchId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;

public record IssueOfflineSalesGrantCommand(
    TenantId tenantId,
    TerminalId terminalId,
    OutletId outletId,
    SalesSessionId salesSessionId,
    UserId sellerUserId,
    OfflineCodeBatchId codeBatchId,
    Instant expiresAt
) implements Command<IssueOfflineSalesGrantResult> {}

