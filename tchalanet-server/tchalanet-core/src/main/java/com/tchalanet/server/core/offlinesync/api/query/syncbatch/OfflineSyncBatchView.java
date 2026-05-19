package com.tchalanet.server.core.offlinesync.api.query.syncbatch;

import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.common.types.id.OfflineSyncBatchId;
import com.tchalanet.server.core.offlinesync.api.model.syncbatch.OfflineSyncBatchStatus;

import java.time.Instant;

public record OfflineSyncBatchView(
    OfflineSyncBatchId id,
    OfflineGrantId grantId,
    String clientBatchId,
    OfflineSyncBatchStatus status,
    Instant receivedAt,
    Instant processedAt,
    int submissionCount,
    int technicalRejectCount,
    int salesAcceptCount,
    int salesRejectCount,
    int reviewCount
) {}
