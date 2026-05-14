package com.tchalanet.server.catalog.tenant.internal.batch;

import com.tchalanet.server.catalog.tenant.api.TenantCatalog;
import com.tchalanet.server.common.job.context.JobTenantBootstrap;
import com.tchalanet.server.common.job.context.JobTenantBootstrapProvider;
import com.tchalanet.server.common.types.id.TenantId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CatalogBatchTenantBootstrapProvider implements JobTenantBootstrapProvider {

    private final TenantCatalog tenantCatalog;

    @Override
    public Optional<JobTenantBootstrap> findBootstrapById(TenantId tenantId) {
        return tenantCatalog.findBootstrapById(tenantId)
            .map(t -> new JobTenantBootstrap(
                t.tenantId(),
                t.code(),
                t.currency(),
                t.timezone()
            ));
    }
}
