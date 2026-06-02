package com.tchalanet.server.platform.tenant.internal.resolver;

import com.tchalanet.server.common.context.tenant.TenantContextInfo;
import com.tchalanet.server.common.context.tenant.TenantContextLookup;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.tenant.api.TenantPreContextLookupApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Pre-context tenant lookup — resolves tenant identity before RLS binding.
 * Uses {@link TenantPreContextLookupApi} (rawDataSource, read-only, cached).
 */
@Component
@RequiredArgsConstructor
public class TenantContextLookupService implements TenantContextLookup {

    private final TenantPreContextLookupApi registry;

    @Override
    public Optional<TenantContextInfo> findByCode(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) return Optional.empty();
        return registry.findByCode(tenantCode.trim().toLowerCase())
            .map(v -> new TenantContextInfo(v.tenantId(), v.code(), v.currency(), v.timezone()));
    }

    @Override
    public Optional<TenantContextInfo> findById(TenantId tenantId) {
        if (tenantId == null) return Optional.empty();
        return registry.findById(tenantId)
            .map(v -> new TenantContextInfo(v.tenantId(), v.code(), v.currency(), v.timezone()));
    }
}
