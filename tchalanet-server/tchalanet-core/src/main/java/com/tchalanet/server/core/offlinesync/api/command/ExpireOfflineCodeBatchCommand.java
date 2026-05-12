package com.tchalanet.server.core.offlinesync.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.OfflineCodeBatchId;
import com.tchalanet.server.common.types.id.TenantId;

public record ExpireOfflineCodeBatchCommand(TenantId tenantId, OfflineCodeBatchId codeBatchId)
    implements Command<ExpireOfflineCodeBatchResult> {}

