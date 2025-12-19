package com.tchalanet.server.core.tenant.application.port.out;

import java.util.UUID;

public interface TenantOutletCheckPort {
    boolean hasActiveOutlets(UUID tenantId);
}

