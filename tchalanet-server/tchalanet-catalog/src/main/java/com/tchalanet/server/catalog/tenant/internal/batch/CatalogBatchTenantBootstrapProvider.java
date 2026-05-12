package com.tchalanet.server.catalog.tenant.internal.batch;

import com.tchalanet.server.catalog.tenant.api.TenantCatalog;
import com.tchalanet.server.common.batch.context.BatchTenantBootstrap;
import com.tchalanet.server.common.batch.context.BatchTenantBootstrapProvider;
import com.tchalanet.server.common.types.id.TenantId;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CatalogBatchTenantBootstrapProvider implements BatchTenantBootstrapProvider {

  private final TenantCatalog tenantCatalog;

  @Override
  public Optional<BatchTenantBootstrap> findBootstrapById(TenantId tenantId) {
    return tenantCatalog
        .findBootstrapById(tenantId)
        .map(view -> new BatchTenantBootstrap(view.tenantId(), view.code(), view.timezone(), view.currency()));
  }
}
