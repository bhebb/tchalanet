package com.tchalanet.server.core.offlinesync.internal.application.port.out;

import com.tchalanet.server.common.types.id.OfflineSaleSubmissionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.offlinesync.api.model.OfflineSubmissionForSalesView;

public interface OfflineSubmissionQueryReaderPort {
    OfflineSubmissionForSalesView getForSalesById(TenantId tenantId, OfflineSaleSubmissionId id);
}
