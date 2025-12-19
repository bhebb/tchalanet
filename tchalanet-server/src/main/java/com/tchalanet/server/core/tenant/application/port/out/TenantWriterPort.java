package com.tchalanet.server.core.tenant.application.port.out;

import com.tchalanet.server.core.tenant.domain.model.Tenant;

import java.util.Optional;
import java.util.UUID;

public interface TenantWriterPort {
    Tenant save(Tenant tenant);
    void setActiveThemeId(UUID tenantId, UUID themeId);
}

