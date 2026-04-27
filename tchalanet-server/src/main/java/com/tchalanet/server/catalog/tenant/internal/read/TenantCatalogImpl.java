package com.tchalanet.server.catalog.tenant.internal.read;

import com.tchalanet.server.catalog.tenant.api.model.TenantBootstrapView;
import com.tchalanet.server.catalog.tenant.api.TenantCatalog;
import com.tchalanet.server.catalog.tenant.api.model.TenantRegistryView;
import com.tchalanet.server.catalog.tenant.api.model.TenantStatsView;
import com.tchalanet.server.common.types.enums.TenantStatus;
import com.tchalanet.server.catalog.tenant.internal.mapper.TenantRegistryMapper;
import com.tchalanet.server.catalog.tenant.internal.persistence.TenantRegistryRepository;
import com.tchalanet.server.common.types.id.TenantId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Tenant catalog implementation (read-only).
 */
@Component
@RequiredArgsConstructor
public class TenantCatalogImpl implements TenantCatalog {

  private final TenantRegistryRepository repository;
  private final TenantRegistryMapper mapper;

  @Override
  public Optional<TenantId> findIdByCode(String codeLower) {
    return repository.findByCodeIgnoreCase(codeLower)
        .map(entity -> TenantId.of(entity.getId()));
  }

  @Override
  public Optional<TenantBootstrapView> findBootstrapByCode(String codeLower) {
    return repository.findByCodeIgnoreCase(codeLower)
        .map(mapper::toBootstrapView);
  }

  @Override
  public Optional<TenantBootstrapView> findBootstrapById(TenantId tenantId) {
    return repository.findByIdNotDeleted(tenantId.value())
        .map(mapper::toBootstrapView);
  }

  @Override
  public Optional<TenantRegistryView> findRegistryById(TenantId tenantId) {
    return repository.findByIdNotDeleted(tenantId.value())
        .map(mapper::toRegistryView);
  }

  @Override
  public Optional<TenantRegistryView> findRegistryByCode(String codeLower) {
    return repository.findByCodeIgnoreCase(codeLower)
        .map(mapper::toRegistryView);
  }

  @Override
  public List<TenantId> listActiveTenantIds() {
    return repository.findAllActiveTenantIds().stream()
        .map(TenantId::of)
        .toList();
  }

  @Override
  public Page<TenantRegistryView> listTenants(Pageable pageable) {
    var entityPage = repository.findAll(pageable);
    return entityPage.map(mapper::toRegistryView);
  }

  @Override
  public TenantStatsView stats() {
    int total = repository.countAll();
    int active = repository.countByStatus(TenantStatus.ACTIVE.name());
    int suspended = repository.countByStatus(TenantStatus.SUSPENDED.name());
    return new TenantStatsView(total, active, suspended);
  }
}
