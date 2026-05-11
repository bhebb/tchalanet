package com.tchalanet.server.core.offlinesync.application.command.model.batch;

import com.tchalanet.server.common.types.id.OfflineBatchId;
import com.tchalanet.server.core.offlinesync.domain.model.OfflineBatchStatus;

public record ReceiveOfflineBatchResult(
    OfflineBatchId batchId,
    OfflineBatchStatus status,
    int receivedCount,
    int readyForSalesCount,
    int technicalRejectCount
) {}

