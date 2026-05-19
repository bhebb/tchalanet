package com.tchalanet.server.core.offlinesync.internal.application.port.out;

import com.tchalanet.server.common.types.id.OfflineCodeBatchId;
import com.tchalanet.server.core.offlinesync.internal.domain.model.codebatch.OfflineCodeBatch;

import java.util.Optional;

public interface OfflineCodeBatchReaderPort {

    Optional<OfflineCodeBatch> findById(OfflineCodeBatchId id);

    /** {@link #findById(OfflineCodeBatchId)} variant that 404s when missing. */
    OfflineCodeBatch getRequired(OfflineCodeBatchId id);
}
