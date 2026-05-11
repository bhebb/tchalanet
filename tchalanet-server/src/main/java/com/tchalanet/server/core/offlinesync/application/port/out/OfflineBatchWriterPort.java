package com.tchalanet.server.core.offlinesync.application.port.out;

import com.tchalanet.server.common.types.id.OfflineBatchId;

public interface OfflineBatchWriterPort {
  OfflineBatchId saveReceivedBatch(Object batch);
  void markSentToSales(OfflineBatchId batchId);
}
