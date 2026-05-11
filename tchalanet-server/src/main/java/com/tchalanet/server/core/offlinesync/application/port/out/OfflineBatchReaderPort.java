package com.tchalanet.server.core.offlinesync.application.port.out;

import com.tchalanet.server.common.types.id.OfflineBatchId;
import com.tchalanet.server.core.offlinesync.domain.model.OfflineBatch;
import java.util.Optional;

public interface OfflineBatchReaderPort {
  Optional<OfflineBatch> findById(OfflineBatchId id);
}

