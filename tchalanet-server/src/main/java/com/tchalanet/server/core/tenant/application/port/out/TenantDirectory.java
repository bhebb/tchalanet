package com.tchalanet.server.core.tenant.application.port.out;
import com.tchalanet.server.common.types.id.TenantId;

import java.util.UUID;

public interface TenantDirectory {
    TenantId requireTenantIdByCode(String tenantCode);

    default boolean isTenantActive(TenantId tenantId) {
        return true;
    }

}

