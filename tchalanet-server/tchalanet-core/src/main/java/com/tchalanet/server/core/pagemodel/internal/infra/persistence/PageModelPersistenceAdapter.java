package com.tchalanet.server.core.pagemodel.internal.infra.persistence;

import com.tchalanet.server.common.types.id.PageModelId;
import com.tchalanet.server.common.types.id.PageModelTemplateId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.pagemodel.internal.application.port.out.PageModelReaderPort;
import com.tchalanet.server.core.pagemodel.internal.application.port.out.PageModelWriterPort;
import com.tchalanet.server.core.pagemodel.internal.domain.model.PageModelInstance;
import com.tchalanet.server.core.pagemodel.internal.domain.model.PageModelStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

// [Phase 2B] suppression de TchContext.currentOrNull() et du filtre tenant applicatif explicite.
// RLS PostgreSQL via app.current_tenant assure l'isolation — aucun WHERE tenant_id = ? côté Java
// (analysis §BLOQUANT)
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
    return repo.findFirstByLogicalIdAndStatusAndDeletedAtIsNull(
            logicalId, PageModelStatus.PUBLISHED)
        .map(PageModelMapper::toDomain);
  }

  @Override
  public List<PageModelInstance> findAllPublishedByLogicalId(String logicalId) {
    return repo
        .findAllByLogicalIdAndStatusAndDeletedAtIsNull(logicalId, PageModelStatus.PUBLISHED)
        .stream()
        .map(PageModelMapper::toDomain)
        .toList();
  }

  @Override
  public List<PageModelInstance> findAllByTemplateId(PageModelTemplateId templateId) {
    return repo.findAllByTemplateIdAndDeletedAtIsNull(templateId.value()).stream()
        .map(PageModelMapper::toDomain)
        .toList();
  }

  @Override
  public Page<PageModelInstance> search(
      Optional<TenantId> tenantId,
      Optional<String> scope,
      Optional<String> logicalId,
      Pageable pageable) {
    Specification<PageModelJpaEntity> specification =
        (root, query, criteriaBuilder) -> criteriaBuilder.isNull(root.get("deletedAt"));
    if (tenantId.isPresent()) {
      specification =
          specification.and(
              (root, query, criteriaBuilder) ->
                  criteriaBuilder.equal(root.get("tenantId"), tenantId.get().value()));
    }
    if (scope.isPresent()) {
      specification =
          specification.and(
              (root, query, criteriaBuilder) ->
                  criteriaBuilder.equal(root.get("scope"), scope.get()));
    }
    if (logicalId.isPresent()) {
      specification =
          specification.and(
              (root, query, criteriaBuilder) ->
                  criteriaBuilder.equal(root.get("logicalId"), logicalId.get()));
    }
    return repo.findAll(specification, pageable).map(PageModelMapper::toDomain);
  }

  @Override
  public PageModelInstance save(PageModelInstance instance) {
    var entity = toManagedEntity(instance);
    var saved = repo.save(entity);
    return PageModelMapper.toDomain(saved);
  }

  @Override
  public List<PageModelInstance> saveAll(List<PageModelInstance> instances) {
    List<PageModelJpaEntity> entities = instances.stream().map(this::toManagedEntity).toList();
    var saved = repo.saveAll(entities);
    return saved.stream().map(PageModelMapper::toDomain).toList();
  }

  /**
   * Reuses the managed row for updates so Hibernate keeps the persisted {@code @Version} value.
   * Rebuilding an entity from the domain aggregate loses that value and makes {@code merge()}
   * report a false optimistic-lock conflict.
   */
  private PageModelJpaEntity toManagedEntity(PageModelInstance instance) {
    var existing = repo.findById(instance.id().value()).orElse(null);
    return PageModelMapper.toEntity(instance, existing);
  }

  @Override
  public void applyTemplateUpdate(
      PageModelTemplateId templateId,
      String logicalId,
      JsonNode newModel,
      int newSchemaVersion,
      UserId actorId) {
    var entities = repo.findAllByTemplateIdAndDeletedAtIsNull(templateId.value());
    var now = java.time.Instant.now();
    UUID actor = actorId != null ? actorId.value() : null;
    var changed = new ArrayList<PageModelJpaEntity>();

    for (var entity : entities) {
      if (entity.getStatus() == PageModelStatus.DRAFT) {
        applyTemplateToDraft(entity, newModel, newSchemaVersion, now, actor);
        changed.add(entity);
        continue;
      }

      if (entity.getStatus() == PageModelStatus.PUBLISHED) {
        var draft =
            repo.findFirstByTenantIdAndLogicalIdAndStatusAndDeletedAtIsNull(
                entity.getTenantId(), logicalId, PageModelStatus.DRAFT);
        if (draft.isPresent()) {
          applyTemplateToDraft(draft.get(), newModel, newSchemaVersion, now, actor);
          changed.add(draft.get());
        } else {
          changed.add(newDraftFromPublished(entity, newModel, newSchemaVersion, now, actor));
        }
      }
    }

    repo.saveAll(changed);
  }

  private static void applyTemplateToDraft(
      PageModelJpaEntity draft,
      JsonNode newModel,
      int newSchemaVersion,
      java.time.Instant now,
      UUID actor) {
    draft.setModel(newModel);
    draft.setSchemaVersion(newSchemaVersion);
    draft.setUpdatedAt(now);
    draft.setUpdatedBy(actor);
  }

  private static PageModelJpaEntity newDraftFromPublished(
      PageModelJpaEntity published,
      JsonNode newModel,
      int newSchemaVersion,
      java.time.Instant now,
      UUID actor) {
    var draft = new PageModelJpaEntity();
    draft.setId(UUID.randomUUID());
    draft.setTenantId(published.getTenantId());
    draft.setCode(nextDraftCode(published.getCode(), newSchemaVersion));
    draft.setLogicalId(published.getLogicalId());
    draft.setName(published.getName() + " new version");
    draft.setSchema(published.getSchema());
    draft.setScope(published.getScope());
    draft.setSlug(published.getSlug());
    draft.setSchemaVersion(newSchemaVersion);
    draft.setModel(newModel);
    draft.setStatus(PageModelStatus.DRAFT);
    draft.setTemplateId(published.getTemplateId());
    draft.setActive(true);
    draft.setCreatedAt(now);
    draft.setCreatedBy(actor);
    draft.setUpdatedAt(now);
    draft.setUpdatedBy(actor);
    return draft;
  }

  private static String nextDraftCode(String publishedCode, int schemaVersion) {
    var base = publishedCode == null || publishedCode.isBlank() ? "page-model" : publishedCode;
    return base + "-v" + schemaVersion + "-" + UUID.randomUUID().toString().substring(0, 8);
  }
}
