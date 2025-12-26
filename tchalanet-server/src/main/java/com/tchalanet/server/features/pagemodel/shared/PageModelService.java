package com.tchalanet.server.features.pagemodel.shared;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.features.pagemodel.shared.template.PageModelTemplateEntity;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PageModelService {

  private final PageModelRepository repository;
  private final ObjectMapper objectMapper;

  private static final UUID DEFAULT_TENANT_UUID =
      UUID.fromString("00000000-0000-0000-0000-000000000000");

  public PageModel loadByLogicalId(UUID tenantId, String logicalId) {
    PageModelEntity entity =
        repository
            .findByTenantIdAndLogicalIdAndDeletedAtIsNull(tenantId, logicalId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException("PageModel not found for logicalId=" + logicalId));
    return toDomain(entity);
  }

  public PageModel loadByTenantScopeSlug(UUID tenantId, String scope, String slug) {
    PageModelEntity entity =
        repository
            .findByTenantIdAndScopeAndSlugAndDeletedAtIsNull(tenantId, scope, slug)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "PageModel not found for scope/slug=" + scope + "/" + slug));
    return toDomain(entity);
  }

  /**
   * Charge le PageModel "effectif" utilisé au runtime, en ne considérant que les PUBLISHED. Ordre
   * de fallback : 1) PUBLISHED pour le tenant courant 2) PUBLISHED pour le tenant "default" 3)
   * template JSON embarqué dans les resources
   */
  public PageModel loadEffectiveModel(UUID tenantId, String logicalId) {
    // 1) PUBLISHED pour le tenant courant
    return repository
        .findByTenantIdAndLogicalIdAndStatusAndDeletedAtIsNull(
            tenantId, logicalId, PageStatus.PUBLISHED)
        .map(this::toDomain)
        // 2) PUBLISHED pour le tenant "default"
        .or(
            () ->
                repository
                    .findByTenantIdAndLogicalIdAndStatusAndDeletedAtIsNull(
                        DEFAULT_TENANT_UUID, logicalId, PageStatus.PUBLISHED)
                    .map(this::toDomain))
        // 3) fallback sur template JSON embarqué
        .orElseGet(() -> loadFromTemplate(logicalId));
  }

  private PageModel loadFromTemplate(String logicalId) {
    // Convention : resources/pages/<logicalId>.json
    String path = "pages/" + logicalId + ".json";
    ClassPathResource resource = new ClassPathResource(path);
    if (!resource.exists()) {
      throw new IllegalStateException(
          "No effective PageModel found and no template for logicalId=" + logicalId);
    }
    try (InputStream is = resource.getInputStream()) {
      return objectMapper.readValue(is, PageModel.class);
    } catch (IOException e) {
      throw new IllegalStateException(
          "Unable to load PageModel template from resources: " + path, e);
    }
  }

  public PageModel toDomain(PageModelEntity entity) {
    try {
      return objectMapper.readValue(entity.getModel(), PageModel.class);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Unable to deserialize PageModel JSON", e);
    }
  }

  // --- New helper methods to manage templateId ---

  public PageModelEntity createFromTemplate(
      UUID tenantId, PageModelEntity template, boolean publish, UUID actorId) {
    PageModelEntity e = new PageModelEntity();
    e.setTenantId(tenantId);
    e.setLogicalId(template.getLogicalId());
    // derive scope/slug from logicalId or template metadata - for now keep logical mapping
    // try to infer scope/slug if present in template.model ?? TODO
    e.setSchemaVersion(template.getSchemaVersion());
    e.setModel(template.getModel());
    e.setTemplateId(template.getTemplateId());
    e.setStatus(publish ? PageStatus.PUBLISHED : PageStatus.DRAFT);
    if (publish) {
      e.setPublishedAt(Instant.now());
    }
    e.setCreatedAt(Instant.now());
    e.setUpdatedAt(Instant.now());
    e.setCreatedBy(actorId);
    e.setUpdatedBy(actorId);
    return repository.save(e);
  }

  public PageModelEntity createFromTemplate(
      UUID tenantId, PageModelTemplateEntity template, boolean publish, UUID actorId) {
    PageModelEntity e = new PageModelEntity();
    e.setTenantId(tenantId);
    e.setLogicalId(template.getLogicalId());
    e.setScope(null);
    e.setSlug(null);
    e.setSchemaVersion(template.getSchemaVersion());
    e.setModel(template.getModelJson());
    e.setTemplateId(template.getId());
    e.setStatus(publish ? PageStatus.PUBLISHED : PageStatus.DRAFT);
    if (publish) {
      e.setPublishedAt(Instant.now());
    }
    e.setCreatedAt(Instant.now());
    e.setUpdatedAt(Instant.now());
    e.setCreatedBy(actorId);
    e.setUpdatedBy(actorId);
    return repository.save(e);
  }

  public PageModelEntity duplicateToTenant(UUID sourceId, UUID targetTenantId, UUID actorId) {
    PageModelEntity src =
        repository
            .findById(sourceId)
            .orElseThrow(
                () -> new IllegalArgumentException("Source PageModel not found: " + sourceId));
    PageModelEntity copy = new PageModelEntity();
    copy.setTenantId(targetTenantId);
    copy.setLogicalId(src.getLogicalId());
    copy.setScope(src.getScope());
    copy.setSlug(src.getSlug());
    copy.setSchemaVersion(src.getSchemaVersion());
    copy.setModel(src.getModel());
    copy.setStatus(PageStatus.DRAFT);
    copy.setTemplateId(src.getTemplateId()); // conserve la référence au template
    copy.setCreatedAt(Instant.now());
    copy.setUpdatedAt(Instant.now());
    copy.setCreatedBy(actorId);
    copy.setUpdatedBy(actorId);
    return repository.save(copy);
  }

  public List<PageModelEntity> findByTemplateId(UUID templateId) {
    return repository.findAllByTemplateIdAndDeletedAtIsNull(templateId);
  }

  public void applyTemplateToInstances(
      UUID templateId, String modelJson, int schemaVersion, UUID actorId, boolean setDraft) {
    List<PageModelEntity> instances = repository.findAllByTemplateIdAndDeletedAtIsNull(templateId);
    Instant now = Instant.now();
    for (PageModelEntity inst : instances) {
      inst.setModel(modelJson);
      inst.setSchemaVersion(schemaVersion);
      if (setDraft) {
        inst.setStatus(PageStatus.DRAFT);
        inst.setPublishedAt(null);
      }
      inst.setUpdatedAt(now);
      inst.setUpdatedBy(actorId);
      repository.save(inst);
    }
  }
}
