package com.tchalanet.server.core.pagemodel.infra.persistence;

import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.core.pagemodel.application.port.PageModelReadPort;
import com.tchalanet.server.core.pagemodel.application.port.PageModelWritePort;
import com.tchalanet.server.core.pagemodel.domain.model.PageModelInstance;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PageModelPersistenceAdapter implements PageModelReadPort, PageModelWritePort {

  private final PageModelJpaRepository repo;

  @Override
  public Optional<PageModelInstance> findById(UUID id) {
    return repo.findById(id).map(PageModelMapper::toDomain);
  }

  @Override
  public Optional<PageModelInstance> findPublishedByLogicalId(String logicalId) {
    // Try tenant-scoped lookup first (some callers may prefer explicit tenant), fallback to RLS-based
    var ctx = TchContext.currentOrNull();
    UUID tenantUuid = ctx == null ? null : ctx.tenantUuid();

    if (tenantUuid != null) {
      var byTenant = repo.findFirstByTenantIdAndLogicalIdAndStatusAndDeletedAtIsNull(tenantUuid, logicalId, "PUBLISHED");
      if (byTenant.isPresent()) return byTenant.map(PageModelMapper::toDomain);
    }

    return repo.findFirstByLogicalIdAndStatusAndDeletedAtIsNull(logicalId, "PUBLISHED").map(PageModelMapper::toDomain);
  }

  @Override
  public List<PageModelInstance> findAllPublishedByLogicalId(String logicalId) {
    return repo.findAllByLogicalIdAndStatusAndDeletedAtIsNull(logicalId, "PUBLISHED").stream().map(PageModelMapper::toDomain).toList();
  }

  @Override
  public List<PageModelInstance> list(UUID tenantIdOrNull, String scopeOrNull, String logicalIdOrNull) {
    // simplistic stub: return all mapped
    return repo.findAll().stream().map(PageModelMapper::toDomain).toList();
  }

  @Override
  public PageModelInstance save(PageModelInstance instance) {
    var entity = PageModelMapper.toEntity(instance, null);
    var saved = repo.save(entity);
    return PageModelMapper.toDomain(saved);
  }

  @Override
  public List<PageModelInstance> saveAll(List<PageModelInstance> instances) {
    List<PageModelJpaEntity> entities = instances.stream().map(d -> PageModelMapper.toEntity(d, null)).toList();
    var saved = repo.saveAll(entities);
    return saved.stream().map(PageModelMapper::toDomain).toList();
  }
}
