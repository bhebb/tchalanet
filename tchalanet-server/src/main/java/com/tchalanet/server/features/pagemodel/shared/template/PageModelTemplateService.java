package com.tchalanet.server.features.pagemodel.shared.template;

import com.tchalanet.server.features.pagemodel.shared.PageModelService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PageModelTemplateService {

  private final PageModelTemplateRepository repository;
  private final PageModelService pageModelService;

  public PageModelTemplateService(
      PageModelTemplateRepository repository, PageModelService pageModelService) {
    this.repository = repository;
    this.pageModelService = pageModelService;
  }

  /** Retourne l'entité template système par défaut si présente. */
  public Optional<PageModelTemplateEntity> loadDefaultTemplateEntity(String logicalId) {
    return repository.findByLogicalIdAndIsDefaultTrueAndIsSystemTrueAndDeletedAtIsNull(logicalId);
  }

  public List<PageModelTemplateEntity> findAllSystemTemplates() {
    return repository.findAllByIsSystemTrueAndDeletedAtIsNull();
  }

  public List<PageModelTemplateEntity> findAllByTenant(UUID tenantId) {
    return repository.findAllByTenantIdAndDeletedAtIsNull(tenantId);
  }

  public Optional<PageModelTemplateEntity> findDefaultByLogicalId(String logicalId) {
    return repository.findByLogicalIdAndIsDefaultTrueAndIsSystemTrueAndDeletedAtIsNull(logicalId);
  }

  public Optional<PageModelTemplateEntity> findById(UUID id) {
    return repository.findById(id).filter(e -> e.getDeletedAt() == null);
  }

  public PageModelTemplateEntity create(PageModelTemplateEntity tpl, UUID actorId) {
    Instant now = Instant.now();
    tpl.setCreatedAt(now);
    tpl.setUpdatedAt(now);
    tpl.setCreatedBy(actorId);
    tpl.setUpdatedBy(actorId);
    // version handling left to JPA / DB
    return repository.save(tpl);
  }

  public PageModelTemplateEntity update(UUID id, PageModelTemplateEntity dto, UUID actorId) {
    PageModelTemplateEntity existing =
        repository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Template not found: " + id));

    if (existing.getDeletedAt() != null) {
      throw new IllegalArgumentException("Template already deleted: " + id);
    }

    existing.setLabel(dto.getLabel());
    existing.setDescription(dto.getDescription());
    existing.setModelJson(dto.getModelJson());
    existing.setSchemaVersion(dto.getSchemaVersion());
    // use Lombok-generated setters for boolean fields
    existing.setSystem(dto.isSystem());
    existing.setDefault(dto.isDefault());
    existing.setTenantId(dto.getTenantId());
    existing.setLogicalId(dto.getLogicalId());
    existing.setUpdatedAt(Instant.now());
    existing.setUpdatedBy(actorId);
    return repository.save(existing);
  }

  /**
   * Met à jour un template puis, si propagate=true, applique le model du template à toutes les
   * PageModel instances liées (safe mode : met les pages en DRAFT pour relecture).
   */
  public PageModelTemplateEntity updateAndPropagate(
      UUID id, PageModelTemplateEntity dto, UUID actorId, boolean propagate) {
    PageModelTemplateEntity updated = update(id, dto, actorId);
    if (propagate) {
      // applique le JSON du template à toutes les instances liées et les met en DRAFT
      pageModelService.applyTemplateToInstances(
          updated.getId(), updated.getModelJson(), updated.getSchemaVersion(), actorId, true);
    }
    return updated;
  }

  public void softDelete(UUID id, UUID actorId) {
    PageModelTemplateEntity existing =
        repository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Template not found: " + id));
    Instant now = Instant.now();
    existing.setDeletedAt(now);
    existing.setUpdatedAt(now);
    existing.setUpdatedBy(actorId);
    repository.save(existing);
  }

  public PageModelTemplateEntity setDefault(UUID id, UUID actorId) {
    PageModelTemplateEntity toDefault =
        repository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Template not found: " + id));
    String logicalId = toDefault.getLogicalId();

    List<PageModelTemplateEntity> siblings =
        repository.findAllByLogicalIdAndDeletedAtIsNull(logicalId);
    Instant now = Instant.now();
    for (PageModelTemplateEntity s : siblings) {
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
}
