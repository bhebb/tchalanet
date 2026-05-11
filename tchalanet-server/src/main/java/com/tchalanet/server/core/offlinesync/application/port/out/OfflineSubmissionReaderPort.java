package com.tchalanet.server.core.offlinesync.application.port.out;

import com.tchalanet.server.common.types.id.OfflineBatchId;
import com.tchalanet.server.common.types.id.OfflineSaleSubmissionId;
import com.tchalanet.server.core.offlinesync.domain.model.OfflineSaleSubmission;
import java.util.List;
import java.util.Optional;

public interface OfflineSubmissionReaderPort {
  List<OfflineSaleSubmission> findReadyForSales(OfflineBatchId batchId);
  Optional<OfflineSaleSubmission> findById(OfflineSaleSubmissionId id);
}
