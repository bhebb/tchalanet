package com.tchalanet.server.catalog.pagemodeltemplate.internal.read;

import com.tchalanet.server.catalog.pagemodeltemplate.api.PageModelTemplateView;
import com.tchalanet.server.catalog.pagemodeltemplate.api.PageModelTemplateCatalog;
import com.tchalanet.server.catalog.pagemodeltemplate.internal.cache.PageModelTemplateCacheNames;
import com.tchalanet.server.catalog.pagemodeltemplate.internal.mapper.PageModelTemplateMapper;
import com.tchalanet.server.catalog.pagemodeltemplate.internal.persistence.PageModelTemplateEntity;
import com.tchalanet.server.catalog.pagemodeltemplate.internal.persistence.PageModelTemplateRepository;
import com.tchalanet.server.common.types.id.PageModelTemplateId;
import com.tchalanet.server.common.types.id.TenantId;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PageModelTemplateCatalogImpl implements PageModelTemplateCatalog {

  private final PageModelTemplateRepository repository;
  private final PageModelTemplateMapper mapper;

  @Override
  @Cacheable(value = PageModelTemplateCacheNames.BY_ID, key = "#id.value()")
  public Optional<PageModelTemplateView> findById(PageModelTemplateId id) {
    return repository.findById(id.value()).filter(e -> e.getDeletedAt() == null).map(mapper::toView);
  }

  @Override
  @Cacheable(value = PageModelTemplateCacheNames.BY_LOGICAL_ID, key = "#logicalId")
  public Optional<PageModelTemplateView> findSystemDefaultByLogicalId(String logicalId) {
    return repository
        .findByLogicalIdAndIsDefaultTrueAndIsSystemTrueAndDeletedAtIsNull(logicalId)
        .map(mapper::toView);
  }

  @Override
  @Cacheable(value = PageModelTemplateCacheNames.BY_TENANT, key = "#tenantId.value()")
  public List<PageModelTemplateView> listByTenant(TenantId tenantId) {
    var entities = repository.findAllByTenantIdAndDeletedAtIsNull(tenantId.value());
    return mapper.toViews(entities);
  }

  @Override
  @Cacheable(value = PageModelTemplateCacheNames.BY_LOGICAL_ID, key = "#logicalId")
  public List<PageModelTemplateView> listSystemTemplates() {
    var entities = repository.findAllByIsSystemTrueAndDeletedAtIsNull();
    return mapper.toViews(entities);
  }
}
