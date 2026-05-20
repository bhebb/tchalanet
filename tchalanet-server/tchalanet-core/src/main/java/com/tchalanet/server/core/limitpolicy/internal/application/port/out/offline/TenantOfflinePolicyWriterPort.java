package com.tchalanet.server.core.limitpolicy.internal.application.port.out.offline;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.limitpolicy.api.model.offline.OfflineLimitPolicy;

public interface TenantOfflinePolicyWriterPort {

    /** Upsert (insert or update) the offline policy for the given tenant. */
    OfflineLimitPolicy upsert(TenantId tenantId, OfflineLimitPolicy policy);
}
