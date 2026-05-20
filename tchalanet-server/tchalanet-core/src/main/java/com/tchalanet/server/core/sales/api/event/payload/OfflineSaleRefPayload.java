package com.tchalanet.server.core.sales.api.event.payload;

import com.tchalanet.server.common.types.id.OfflineSyncBatchId;
import com.tchalanet.server.common.types.id.OfflineCodeBatchId;
import com.tchalanet.server.common.types.id.OfflineSubmissionId;
import com.tchalanet.server.core.sales.api.model.status.OfflineSyncStatus;

import java.time.Instant;

public record OfflineSaleRefPayload(
    OfflineSubmissionId submissionId,
    OfflineSyncBatchId batchId,
    OfflineCodeBatchId codeBatchId,
    String offlineCode,
    String clientSaleId,
    long localSequence,
    Instant soldAtDevice,
    OfflineSyncStatus syncStatus
) {
}
