package com.tchalanet.server.features.pagemodel;

import static com.tchalanet.server.common.constant.CommonConstants.DEFAULT_TENANT_UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.catalog.pagemodeltemplate.api.model.PageModelTemplateView;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.util.JsonUtils;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PageModelService {

  private final PageModelRepository repository;
  private final JsonUtils jsonUtils;

  public PageModel loadByLogicalId(String logicalId) {
    PageModelEntity entity =
        repository
            .findByLogicalIdAndDeletedAtIsNull(logicalId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException("PageModel not found for logicalId=" + logicalId));
    return toDomain(entity);
  }

  public PageModel loadByTenantScopeSlug(UUID tenantId, String scope, String slug) {
    PageModelEntity entity =
        repository
            .findByScopeAndSlugAndDeletedAtIsNull(scope, slug)
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

      // 1) tenant courant (si fourni)
      var tenantPublished =
          tenantId == null
              ? Optional.<PageModelEntity>empty()
              : repository.findByTenantIdAndLogicalIdAndStatusAndDeletedAtIsNull(
              tenantId, logicalId, PageStatus.PUBLISHED);

      if (tenantPublished.isPresent()) return toDomain(tenantPublished.get());

      // 2) default tenant
      var defaultPublished =
          repository.findByTenantIdAndLogicalIdAndStatusAndDeletedAtIsNull(
              DEFAULT_TENANT_UUID, logicalId, PageStatus.PUBLISHED);

      if (defaultPublished.isPresent()) return toDomain(defaultPublished.get());

      // 3) resources
      return loadFromTemplate(logicalId);
  }
  private PageModel loadFromTemplate(String logicalId) {
    // Try multiple conventional locations (older code used "pages/", project uses "pagemodel/")
    String[] candidatePaths = new String[] {"pagemodel/" + logicalId + ".json"};

    ClassPathResource resource = null;
    String foundPath = null;
    for (String path : candidatePaths) {
      ClassPathResource r = new ClassPathResource(path);
      if (r.exists()) {
        resource = r;
        foundPath = path;
        break;
      }
    }

    if (resource == null) {
      throw new IllegalStateException(
          "No effective PageModel found and no template for logicalId="
              + logicalId
              + ". Attempted paths: pages/"
              + logicalId
              + ".json, pagemodel/"
              + logicalId
              + ".json");
    }

    try (InputStream is = resource.getInputStream()) {
      return jsonUtils.readValue(is, PageModel.class);
    } catch (IOException e) {
      throw new IllegalStateException(
          "Unable to load PageModel template from resources: " + foundPath, e);
    }
  }

  public PageModel toDomain(PageModelEntity entity) {
    return jsonUtils.treeToValue(entity.getModel(), PageModel.class);
  }

  // --- New helper methods to manage templateId ---

  public PageModelEntity createFromTemplate(PageModelTemplateView template, boolean publish) {
    // Provide a minimal TchRequestContext for startup seeding to satisfy RLS and auditing
    var e = new PageModelEntity();
    // default tenant for system templates
    e.setTenantId(DEFAULT_TENANT_UUID);
    e.setCode(template.code());
    e.setLogicalId(template.logicalId());
    e.setName(template.name());
    e.setScope(template.level().name());
    e.setSlug(template.logicalId());
    e.setSchemaVersion(template.schemaVersion());
    // ensure non-null JSON
    e.setSchema(template.schema() == null ? jsonUtils.emptyObjectNode() : template.schema());
    e.setModel(template.model() == null ? jsonUtils.emptyObjectNode() : template.model());
    e.setTemplateId(template.id().value());
    e.setStatus(publish ? PageStatus.PUBLISHED : PageStatus.DRAFT);
    if (publish) e.setPublishedAt(Instant.now());
    return repository.save(e);
  }

  public PageModelEntity createFromTemplate(
      UUID tenantId, PageModelTemplateView template, boolean publish, UUID actorId) {
    PageModelEntity e = new PageModelEntity();
    e.setTenantId(tenantId);
    e.setCode(template.code());
    e.setLogicalId(template.logicalId());
    e.setName(template.name());
    e.setScope(null);
    e.setSlug(null);
    e.setSchemaVersion(template.schemaVersion());
    e.setSchema(template.schema() == null ? jsonUtils.emptyObjectNode() : template.schema());
    e.setModel(template.model() == null ? jsonUtils.emptyObjectNode() : template.model());
    e.setTemplateId(template.id().value());
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

  @Transactional
  public void applyTemplateToInstances(
      UUID templateId, JsonNode modelJson, int schemaVersion, UUID actorId, boolean setDraft) {
    List<PageModelEntity> instances = repository.findAllByTemplateIdAndDeletedAtIsNull(templateId);
    Instant now = Instant.now();
    for (PageModelEntity inst : instances) {
      inst.setModel(modelJson == null ? jsonUtils.emptyObjectNode() : modelJson);
      inst.setSchemaVersion(schemaVersion);
      if (setDraft) {
        inst.setStatus(PageStatus.DRAFT);
        inst.setPublishedAt(null);
      }
      inst.setUpdatedAt(now);
      inst.setUpdatedBy(actorId);
    }
    repository.saveAll(instances);
  }
}
