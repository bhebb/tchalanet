package com.tchalanet.server.core.offlinesync.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.context.operational.OperationalContextHint;
import com.tchalanet.server.common.types.id.OfflineCodeBatchId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;

public record IssueOfflineSalesGrantCommand(
    TenantId tenantId,
    UserId sellerUserId,
    OperationalContextHint operationalContext,
    OfflineCodeBatchId codeBatchId,
    Instant expiresAt
) implements Command<IssueOfflineSalesGrantResult> {}
