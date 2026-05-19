package com.tchalanet.server.core.offlinesync.internal.application.port.out;

import com.tchalanet.server.core.offlinesync.internal.domain.model.syncbatch.OfflineSyncBatch;

public interface OfflineSyncBatchWriterPort {

    OfflineSyncBatch save(OfflineSyncBatch batch);
}
