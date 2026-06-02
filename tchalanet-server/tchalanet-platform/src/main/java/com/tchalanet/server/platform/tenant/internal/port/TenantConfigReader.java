package com.tchalanet.server.platform.tenant.internal.port;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.tenant.api.model.view.TenantInternalSettings;
import com.tchalanet.server.platform.tenant.internal.domain.TenantConfig;

/**
 * Lean port for reading tenant internal settings directly from persistence.
 * Use this instead of TenantConfigApi when you only need the config sub-sections
 * (rules, document, communication) without the full tenant lifecycle view.
 */
public interface TenantConfigReader {
    TenantInternalSettings getInternalSettings(TenantId tenantId);
}
