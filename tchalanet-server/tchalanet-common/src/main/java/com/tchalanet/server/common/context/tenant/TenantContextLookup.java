package com.tchalanet.server.common.context.tenant;

import com.tchalanet.server.common.types.id.TenantId;
import java.util.Optional;

public interface TenantContextLookup {

    Optional<TenantContextInfo> findById(TenantId tenantId);

    Optional<TenantContextInfo> findByCode(String tenantCode);
}
