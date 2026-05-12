package com.tchalanet.server.core.offlinesync.api.command;

import com.tchalanet.server.common.types.id.OfflineBatchId;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineBatchStatus;

public record ReceiveOfflineBatchResult(
    OfflineBatchId batchId,
    OfflineBatchStatus status,
    int receivedCount,
    int readyForSalesCount,
    int technicalRejectCount
) {}

