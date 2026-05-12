package com.tchalanet.server.core.offlinesync.internal.application.port.out;

import com.tchalanet.server.common.types.id.OfflineBatchId;
import com.tchalanet.server.core.offlinesync.domain.model.OfflineBatch;

public interface OfflineBatchWriterPort {
  OfflineBatchId saveReceivedBatch(OfflineBatch batch);
  void markSentToSales(OfflineBatchId batchId);
}
