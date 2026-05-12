package com.tchalanet.server.core.offlinesync.internal.application.port.out;

import com.tchalanet.server.common.types.id.OfflineBatchId;
import com.tchalanet.server.common.types.id.OfflineSaleSubmissionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineSubmissionStatus;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineSaleSubmission;
import java.util.List;
import java.util.Optional;

public interface OfflineSubmissionReaderPort {
  List<OfflineSaleSubmission> findReadyForSales(OfflineBatchId batchId);
  Optional<OfflineSaleSubmission> findById(OfflineSaleSubmissionId id);
  List<OfflineSaleSubmission> listByBatch(OfflineBatchId batchId, OfflineSubmissionStatus status);
  long countByTenantAndStatus(TenantId tenantId, OfflineSubmissionStatus status);
}
