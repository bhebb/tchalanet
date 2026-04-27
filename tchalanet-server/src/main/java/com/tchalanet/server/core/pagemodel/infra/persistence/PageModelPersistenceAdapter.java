package com.tchalanet.server.core.pagemodel.infra.persistence;

import tools.jackson.databind.JsonNode;
import com.tchalanet.server.common.types.id.PageModelId;
import com.tchalanet.server.common.types.id.PageModelTemplateId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.pagemodel.application.port.out.PageModelReaderPort;
import com.tchalanet.server.core.pagemodel.application.port.out.PageModelWriterPort;
import com.tchalanet.server.core.pagemodel.domain.model.PageModelInstance;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

// [Phase 2B] suppression de TchContext.currentOrNull() et du filtre tenant applicatif explicite.
// RLS PostgreSQL via app.current_tenant assure l'isolation — aucun WHERE tenant_id = ? côté Java (analysis §BLOQUANT)
// [Phase 3C] findById(UUID) → findById(PageModelId) (analysis §MAJEUR typed_ids §2)
// [Phase 3B] search(...) ajouté pour ListPageModelsHandler
@Component
@RequiredArgsConstructor
public class PageModelPersistenceAdapter implements PageModelReaderPort, PageModelWriterPort {

  private final PageModelJpaRepository repo;

  @Override
  public Optional<PageModelInstance> findById(PageModelId id) {
    return repo.findById(id.value()).map(PageModelMapper::toDomain);
  }

  @Override
  public Optional<PageModelInstance> findPublishedByLogicalId(String logicalId) {
    // RLS assure l'isolation tenant — pas de filtre tenant_id explicite
    return repo.findFirstByLogicalIdAndStatusAndDeletedAtIsNull(logicalId, "PUBLISHED")
        .map(PageModelMapper::toDomain);
  }

  @Override
  public List<PageModelInstance> findAllPublishedByLogicalId(String logicalId) {
    return repo.findAllByLogicalIdAndStatusAndDeletedAtIsNull(logicalId, "PUBLISHED")
        .stream().map(PageModelMapper::toDomain).toList();
  }


  @Override
  public Page<PageModelInstance> search(
      Optional<TenantId> tenantId,
      Optional<String> scope,
      Optional<String> logicalId,
      Pageable pageable) {
    return repo.findAllByDeletedAtIsNull(pageable).map(PageModelMapper::toDomain);
  }

  @Override
  public PageModelInstance save(PageModelInstance instance) {
    var entity = PageModelMapper.toEntity(instance, null);
    var saved = repo.save(entity);
    return PageModelMapper.toDomain(saved);
  }

  @Override
  public List<PageModelInstance> saveAll(List<PageModelInstance> instances) {
    List<PageModelJpaEntity> entities = instances.stream()
        .map(d -> PageModelMapper.toEntity(d, null)).toList();
    var saved = repo.saveAll(entities);
    return saved.stream().map(PageModelMapper::toDomain).toList();
  }

  // [Phase 4C] propagation template → instances DRAFT (analysis §gap)
  @Override
  public void applyTemplateUpdate(PageModelTemplateId templateId, JsonNode newModel,
      int newSchemaVersion, UserId actorId) {
    var entities = repo.findAllByTemplateIdAndDeletedAtIsNull(templateId.value());
    var now = java.time.Instant.now();
    UUID actor = actorId != null ? actorId.value() : null;
    for (var entity : entities) {
      entity.setModel(newModel);
      entity.setSchemaVersion(newSchemaVersion);
      // Repasser en DRAFT : le modèle a changé, une republication explicite est requise
      entity.setStatus(com.tchalanet.server.core.pagemodel.domain.model.PageModelStatus.DRAFT);
      entity.setUpdatedAt(now);
      entity.setUpdatedBy(actor);
    }
    repo.saveAll(entities);
  }
}
