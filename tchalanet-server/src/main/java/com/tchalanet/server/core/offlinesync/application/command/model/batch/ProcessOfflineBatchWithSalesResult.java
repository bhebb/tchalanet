package com.tchalanet.server.core.offlinesync.application.command.model.batch;

import com.tchalanet.server.common.types.id.OfflineBatchId;

public record ProcessOfflineBatchWithSalesResult(
    OfflineBatchId batchId,
    int acceptedCount,
    int rejectedCount,
    int reviewCount,
    int conflictCount
) {}

