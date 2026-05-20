package com.tchalanet.server.core.offlinesync.internal.application.port.out;

import com.tchalanet.server.core.offlinesync.internal.domain.model.codebatch.OfflineCodeBatch;

public interface OfflineCodeBatchWriterPort {

    OfflineCodeBatch save(OfflineCodeBatch batch);
}
