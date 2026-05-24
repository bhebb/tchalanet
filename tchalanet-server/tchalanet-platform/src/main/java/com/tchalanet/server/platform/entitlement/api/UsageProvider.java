package com.tchalanet.server.platform.entitlement.api;

import com.tchalanet.server.common.types.id.TenantId;

public interface UsageProvider {

    boolean supports(String usageKey);

    int currentUsage(TenantId tenantId, String usageKey);
}
