package com.tchalanet.server.features.pagemodel.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.features.pagemodel.admin.dto.PageModelAdminDetailDto;
import com.tchalanet.server.features.pagemodel.admin.dto.PageModelAdminListItemDto;
import com.tchalanet.server.features.pagemodel.admin.dto.PageModelAdminUpsertRequest;
import com.tchalanet.server.features.pagemodel.shared.PageModel;
import com.tchalanet.server.features.pagemodel.shared.PageModelEntity;
import com.tchalanet.server.features.pagemodel.shared.PageModelRepository;
import com.tchalanet.server.features.pagemodel.shared.PageStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PageModelAdminService {

  private final PageModelRepository repository;
  private final ObjectMapper objectMapper;
  private final TchContextResolver contextResolver;

  public List<PageModelAdminListItemDto> list(UUID tenantId, String scope, String logicalId) {
    return repository.findByDeletedAtIsNull().stream()
        .map(this::toListItemDto)
        .collect(Collectors.toList());
  }

  public PageModelAdminDetailDto get(UUID id) {
    var holder = contextResolver.currentOrNull();
    UUID tenantId = holder != null ? holder.tenantUuid() : null;
    PageModelEntity entity =
        repository
            .findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new IllegalArgumentException("PageModel not found: " + id));
    return toDetailDto(entity);
  }

  /**
   * Charge un PageModel d'administration par logicalId ("type") pour le tenant courant. Utile si on
   * veut travailler "par type" de page.
   */
  public PageModelAdminDetailDto getByLogicalId(String logicalId) {
    var holder = contextResolver.currentOrNull();
    UUID tenantId = holder != null ? holder.tenantUuid() : null;
    PageModelEntity entity =
        repository
            .findByLogicalIdAndDeletedAtIsNull(logicalId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "PageModel not found for tenant/logicalId: " + tenantId + "/" + logicalId));
    return toDetailDto(entity);
  }

  public PageModelAdminDetailDto upsert(PageModelAdminUpsertRequest request) {
    var holder = contextResolver.currentOrNull();
    UUID tenantId = holder != null ? holder.tenantUuid() : null;
    Instant now = Instant.now();

    PageModelEntity entity =
        request.id() != null
            ? repository.findByIdAndDeletedAtIsNull(request.id()).orElseGet(PageModelEntity::new)
            : new PageModelEntity();

    if (entity.getId() == null) {
      entity.setId(UUID.randomUUID());
      entity.setCreatedAt(now);
      entity.setTenantId(tenantId);
      entity.setStatus(PageStatus.DRAFT);
    }

    entity.setLogicalId(request.logicalId());
    entity.setScope(request.scope());
    entity.setSlug(request.slug());
    entity.setSchemaVersion(request.schemaVersion());
    entity.setModel(serializeModel(request.model()));
    entity.setUpdatedAt(now);
    entity.setDeletedAt(null); // un upsert "réactive" éventuellement une page supprimée

    PageModelEntity saved = repository.save(entity);
    return toDetailDto(saved);
  }

  public void delete(UUID id) {
    PageModelEntity entity =
        repository
            .findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new IllegalArgumentException("PageModel not found: " + id));
    Instant now = Instant.now();
    entity.setDeletedAt(now);
    entity.setUpdatedAt(now);
    repository.save(entity);
  }

  public PageModelAdminDetailDto duplicate(
      UUID id, UUID tenantId, String newLogicalId, String newSlug) {

    var source =
        repository
            .findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new IllegalArgumentException("PageModel not found: " + id));

    var clone = new PageModelEntity();
    clone.setTenantId(tenantId);
    clone.setLogicalId(newLogicalId != null ? newLogicalId : source.getLogicalId() + "_copy");
    clone.setScope(source.getScope());
    clone.setSlug(newSlug != null ? newSlug : source.getSlug() + "-copy");
    clone.setSchemaVersion(source.getSchemaVersion());
    clone.setModel(source.getModel());
    clone.setStatus(PageStatus.DRAFT);
    clone.setDeletedAt(null);

    var saved = repository.save(clone);
    return toDetailDto(saved);
  }

  public PageModelAdminDetailDto preview(UUID id) {
    var holder = contextResolver.currentOrNull();
    UUID tenantId = holder != null ? holder.tenantUuid() : null;
    PageModelEntity entity =
        repository
            .findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new IllegalArgumentException("PageModel not found: " + id));
    return toDetailDto(entity);
  }

  private JsonNode serializeModel(PageModel model) {
    return objectMapper.valueToTree(model);
  }

  private PageModelAdminListItemDto toListItemDto(PageModelEntity e) {
    PageModel model = deserializeModel(e.getModel());
    String title = model.meta() != null ? model.meta().id() : e.getLogicalId();
    return new PageModelAdminListItemDto(
        e.getId(),
        e.getLogicalId(),
        e.getScope(),
        e.getSlug(),
        e.getSchemaVersion(),
        title,
        model.meta() != null ? model.meta().langs() : List.of(),
        e.getUpdatedAt());
  }

  private PageModelAdminDetailDto toDetailDto(PageModelEntity e) {
    PageModel model = deserializeModel(e.getModel());
    return new PageModelAdminDetailDto(
        e.getId(),
        e.getTenantId(),
        e.getLogicalId(),
        e.getScope(),
        e.getSlug(),
        e.getSchemaVersion(),
        model,
        e.getCreatedAt(),
        e.getUpdatedAt());
  }

  private PageModel deserializeModel(JsonNode json) {
    try {
      return objectMapper.treeToValue(json, PageModel.class);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Unable to deserialize PageModel", e);
    }
  }

  public PageModelAdminDetailDto publish(UUID id) {
    var holder = contextResolver.currentOrNull();
    var tenantId = holder != null ? holder.tenantUuid() : null;
    var entity =
        repository
            .findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new IllegalArgumentException("PageModel not found: " + id));
    entity.setStatus(PageStatus.PUBLISHED);
    var saved = repository.save(entity);
    return toDetailDto(saved);
  }
}
