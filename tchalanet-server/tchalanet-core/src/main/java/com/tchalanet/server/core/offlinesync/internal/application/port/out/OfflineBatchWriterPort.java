package com.tchalanet.server.core.offlinesync.internal.application.port.out;

import com.tchalanet.server.common.types.id.OfflineBatchId;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineBatch;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineSaleSubmission;
import java.util.List;

public interface OfflineBatchWriterPort {
  OfflineBatchId saveReceivedBatch(OfflineBatch batch, List<OfflineSaleSubmission> submissions);
  void markSentToSales(OfflineBatchId batchId);
}
