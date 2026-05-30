package com.tchalanet.server.platform.tenantconfig.internal.port;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.tenantconfig.api.model.view.TenantInternalSettings;

/**
 * Lean port for reading tenant internal settings directly from persistence.
 * Use this instead of TenantConfigApi when you only need the config sub-sections
 * (rules, document, communication) without the full tenant lifecycle view.
 */
public interface TenantConfigReader {
    TenantInternalSettings getInternalSettings(TenantId tenantId);
}
