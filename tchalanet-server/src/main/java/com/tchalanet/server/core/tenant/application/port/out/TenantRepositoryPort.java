package com.tchalanet.server.core.tenant.application.port.out;

import java.util.UUID;

public interface TenantRepositoryPort {
    boolean hasActiveOutlets(UUID tenantId);
    void archiveTenant(UUID tenantId, String reason);
}

