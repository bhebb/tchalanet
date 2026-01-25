package com.tchalanet.server.catalog.pagemodeltemplate.internal.write;

import com.tchalanet.server.catalog.pagemodeltemplate.internal.cache.PageModelTemplateCacheNames;
import com.tchalanet.server.catalog.pagemodeltemplate.internal.mapper.PageModelTemplateMapper;
import com.tchalanet.server.catalog.pagemodeltemplate.internal.persistence.PageModelTemplateEntity;
import com.tchalanet.server.catalog.pagemodeltemplate.internal.persistence.PageModelTemplateRepository;
import com.tchalanet.server.common.types.id.PageModelTemplateId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PageModelTemplateAdminService {

  private final PageModelTemplateRepository repository;
  private final PageModelTemplateMapper mapper;

  @Transactional(readOnly = true)
  public TchPage<?> search(Object criteria, TchPageRequest pageRequest) {
    // For brevity, implement later if needed
    return TchPage.empty();
  }

  @Transactional(readOnly = true)
  public Optional<PageModelTemplateEntity> findById(PageModelTemplateId id) {
    return repository.findById(id.value()).filter(e -> e.getDeletedAt() == null);
  }

  @Transactional
  @CacheEvict(
      cacheNames = {
        PageModelTemplateCacheNames.BY_ID,
        PageModelTemplateCacheNames.BY_LOGICAL_ID,
        PageModelTemplateCacheNames.BY_TENANT
      },
      allEntries = true)
  public PageModelTemplateEntity create(PageModelTemplateEntity entity) {
    return repository.save(entity);
  }

  @Transactional
  @CacheEvict(
      cacheNames = {
        PageModelTemplateCacheNames.BY_ID,
        PageModelTemplateCacheNames.BY_LOGICAL_ID,
        PageModelTemplateCacheNames.BY_TENANT
      },
      allEntries = true)
  public PageModelTemplateEntity update(UUID id, PageModelTemplateEntity dto, UUID actorId) {
    var existing = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Template not found: " + id));
    if (existing.getDeletedAt() != null) throw new IllegalArgumentException("Template already deleted: " + id);
    existing.setLabel(dto.getLabel());
    existing.setDescription(dto.getDescription());
    existing.setModel(dto.getModel());
    existing.setSchemaVersion(dto.getSchemaVersion());
    existing.setSystem(dto.isSystem());
    existing.setDefault(dto.isDefault());
    existing.setTenantId(dto.getTenantId());
    existing.setLogicalId(dto.getLogicalId());
    existing.setUpdatedAt(Instant.now());
    existing.setUpdatedBy(actorId);
    return repository.save(existing);
  }

  @Transactional
  @CacheEvict(
      cacheNames = {
        PageModelTemplateCacheNames.BY_ID,
        PageModelTemplateCacheNames.BY_LOGICAL_ID,
        PageModelTemplateCacheNames.BY_TENANT
      },
      allEntries = true)
  public void softDelete(UUID id, UUID actorId) {
    var existing = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Template not found: " + id));
    Instant now = Instant.now();
    existing.setDeletedAt(now);
    existing.setUpdatedAt(now);
    existing.setUpdatedBy(actorId);
    repository.save(existing);
  }

  @Transactional
  @CacheEvict(
      cacheNames = {
        PageModelTemplateCacheNames.BY_ID,
        PageModelTemplateCacheNames.BY_LOGICAL_ID,
        PageModelTemplateCacheNames.BY_TENANT
      },
      allEntries = true)
  public PageModelTemplateEntity setDefault(UUID id, UUID actorId) {
    var toDefault = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Template not found: " + id));
    String logicalId = toDefault.getLogicalId();
    var siblings = repository.findAllByLogicalIdAndDeletedAtIsNull(logicalId);
    Instant now = Instant.now();
    for (var s : siblings) {
      if (s.isDefault() && s.isSystem() && !s.getId().equals(id)) {
        s.setDefault(false);
        s.setUpdatedAt(now);
        s.setUpdatedBy(actorId);
        repository.save(s);
      }
    }
    toDefault.setDefault(true);
    toDefault.setUpdatedAt(Instant.now());
    toDefault.setUpdatedBy(actorId);
    return repository.save(toDefault);
  }

  // Mapping helpers used by controllers (internal only)
  public PageModelTemplateView mapToView(PageModelTemplateEntity e) {
    return mapper.toView(e);
  }

  public PageModelTemplateEntity mapToEntity(PageModelTemplateView v) {
    PageModelTemplateEntity e = new PageModelTemplateEntity();
    // minimal mapping: keep modelJson as string -> JsonNode conversion left to service consumer
    e.setCode(v.code());
    e.setTenantId(v.tenantId() == null ? null : v.tenantId().value());
    e.setLogicalId(v.logicalId());
    e.setName(v.name());
    e.setLabel(v.label());
    e.setDescription(v.description());
    e.setSchemaVersion(v.schemaVersion());
    // modelJson -> model conversion omitted (requires ObjectMapper)
    e.setDefault(v.isDefault());
    e.setSystem(v.isSystem());
    return e;
  }

  // View-based helpers (internal)
  public Optional<com.tchalanet.server.catalog.pagemodeltemplate.api.PageModelTemplateView> findViewById(PageModelTemplateId id) {
    return findById(id).map(mapper::toView);
  }

  public com.tchalanet.server.catalog.pagemodeltemplate.api.PageModelTemplateView createFromView(com.tchalanet.server.catalog.pagemodeltemplate.api.PageModelTemplateView view) {
    var entity = mapToEntity(view);
    var created = create(entity);
    return mapToView(created);
  }

  public com.tchalanet.server.catalog.pagemodeltemplate.api.PageModelTemplateView updateFromView(PageModelTemplateId id, com.tchalanet.server.catalog.pagemodeltemplate.api.PageModelTemplateView view, UUID actorId) {
    var dto = mapToEntity(view);
    var updated = update(id.value(), dto, actorId);
    return mapToView(updated);
  }
}
