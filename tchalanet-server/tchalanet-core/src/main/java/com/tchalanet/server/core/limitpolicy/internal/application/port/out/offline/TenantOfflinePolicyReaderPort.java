package com.tchalanet.server.core.limitpolicy.internal.application.port.out.offline;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.limitpolicy.api.model.offline.OfflineLimitPolicy;

import java.util.Optional;

public interface TenantOfflinePolicyReaderPort {

    /**
     * @return the tenant-specific offline policy, or empty if none configured (caller
     *         falls back to the global defaults from
     *         {@link com.tchalanet.server.core.limitpolicy.internal.infra.config.OfflineLimitPolicyProperties}).
     */
    Optional<OfflineLimitPolicy> findByTenantId(TenantId tenantId);
}
