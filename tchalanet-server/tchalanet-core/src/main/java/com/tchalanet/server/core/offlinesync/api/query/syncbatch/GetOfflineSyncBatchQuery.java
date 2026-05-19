package com.tchalanet.server.core.offlinesync.api.query.syncbatch;

import com.tchalanet.server.common.types.id.OfflineSyncBatchId;
import com.tchalanet.server.common.types.id.TenantId;

public record GetOfflineSyncBatchQuery(TenantId tenantId, OfflineSyncBatchId syncBatchId) {}
