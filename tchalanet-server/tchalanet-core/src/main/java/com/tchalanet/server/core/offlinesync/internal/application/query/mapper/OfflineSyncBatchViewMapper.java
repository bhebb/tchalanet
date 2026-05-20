package com.tchalanet.server.core.offlinesync.internal.application.query.mapper;

import com.tchalanet.server.core.offlinesync.api.query.syncbatch.OfflineSyncBatchView;
import com.tchalanet.server.core.offlinesync.internal.domain.model.syncbatch.OfflineSyncBatch;

public final class OfflineSyncBatchViewMapper {

    private OfflineSyncBatchViewMapper() {}

    public static OfflineSyncBatchView toView(OfflineSyncBatch batch) {
        return new OfflineSyncBatchView(
            batch.identity().id(),
            batch.identity().grantId(),
            batch.identity().clientBatchId(),
            batch.lifecycle().status(),
            batch.lifecycle().receivedAt(),
            batch.lifecycle().processedAt(),
            batch.counters().submissionCount(),
            batch.counters().technicalRejectCount(),
            batch.counters().salesAcceptCount(),
            batch.counters().salesRejectCount(),
            batch.counters().reviewCount()
        );
    }
}
