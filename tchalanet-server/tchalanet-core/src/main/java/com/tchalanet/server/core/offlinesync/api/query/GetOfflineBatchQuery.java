package com.tchalanet.server.core.offlinesync.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.OfflineBatchId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.offlinesync.domain.model.OfflineBatch;

public record GetOfflineBatchQuery(TenantId tenantId, OfflineBatchId batchId)
    implements Query<OfflineBatch> {}

