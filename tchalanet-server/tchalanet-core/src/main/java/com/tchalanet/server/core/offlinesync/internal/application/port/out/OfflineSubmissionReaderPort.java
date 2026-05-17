package com.tchalanet.server.core.offlinesync.internal.application.port.out;

import com.tchalanet.server.common.types.id.OfflineSaleSubmissionId;
import com.tchalanet.server.common.types.id.OfflineSalesGrantId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.offlinesync.api.model.OfflineSubmissionStatus;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineSubmission;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OfflineSubmissionReaderPort {
    OfflineSubmission getById(OfflineSaleSubmissionId id);
    Optional<OfflineSubmission> findByGrantAndClientSaleId(OfflineSalesGrantId grantId, String clientSaleId);
    List<OfflineSubmission> findReadyForDispatch(TenantId tenantId, int limit, Instant now);
}
