package com.tchalanet.server.core.sales.api.event.payload;

import com.tchalanet.server.common.types.id.OfflineBatchId;
import com.tchalanet.server.common.types.id.OfflineCodeBatchId;
import com.tchalanet.server.common.types.id.OfflineSaleSubmissionId;
import com.tchalanet.server.core.sales.api.model.status.OfflineSyncStatus;

import java.time.Instant;

public record OfflineSaleRefPayload(
    OfflineSaleSubmissionId submissionId,
    OfflineBatchId batchId,
    OfflineCodeBatchId codeBatchId,
    String offlineCode,
    String clientSaleId,
    long localSequence,
    Instant soldAtDevice,
    OfflineSyncStatus syncStatus
) {
}
