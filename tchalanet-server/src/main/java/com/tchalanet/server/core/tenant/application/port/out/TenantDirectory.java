package com.tchalanet.server.core.tenant.application.port.out;

import java.util.UUID;

public interface TenantDirectory {
    UUID requireTenantIdByCode(String tenantCode);

    default boolean isTenantActive(UUID tenantId) {
        return true;
    }

}

