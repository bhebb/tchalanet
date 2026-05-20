package com.tchalanet.server.platform.tenantconfig.internal.context;

import com.tchalanet.server.catalog.tenant.api.TenantCatalog;
import com.tchalanet.server.catalog.tenant.api.model.TenantBootstrapView;
import com.tchalanet.server.common.context.tenant.TenantContextInfo;
import com.tchalanet.server.common.context.tenant.TenantContextLookup;
import com.tchalanet.server.common.types.id.TenantId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TenantConfigContextLookup implements TenantContextLookup {

    private final TenantCatalog tenantCatalog;

    @Override
    public Optional<TenantContextInfo> findById(TenantId tenantId) {
        return tenantCatalog.findBootstrapById(tenantId)
            .map(this::toTenantContextInfo);
    }

    @Override
    public Optional<TenantContextInfo> findByCode(String tenantCode) {
        return tenantCatalog.findBootstrapByCode(tenantCode)
            .map(this::toTenantContextInfo);
    }

    private TenantContextInfo toTenantContextInfo(TenantBootstrapView view) {
        return new TenantContextInfo(
            view.tenantId(),
            view.code(),
            view.currency(),
            view.timezone());
    }
}
