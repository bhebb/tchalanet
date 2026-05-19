package com.tchalanet.server.core.sales.api.model.origin;

import com.tchalanet.server.common.types.id.OfflineSyncBatchId;
import com.tchalanet.server.common.types.id.OfflineCodeBatchId;
import com.tchalanet.server.common.types.id.OfflineSubmissionId;
import com.tchalanet.server.core.sales.api.model.status.OfflineSyncStatus;

import java.time.Instant;

public record OfflineSaleRef(
    OfflineSubmissionId submissionId,
    OfflineSyncBatchId batchId,
    OfflineCodeBatchId codeBatchId,
    String offlineCode,
    String clientSaleId,
    long localSequence,
    Instant soldAtDevice,
    OfflineSyncStatus syncStatus
) {}

