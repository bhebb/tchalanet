package com.tchalanet.server.core.pagemodel.internal.infra.persistence;

import com.tchalanet.server.common.types.id.PageModelTemplateId;
import tools.jackson.databind.JsonNode;
import com.tchalanet.server.core.pagemodel.internal.domain.model.PageModelInstance;
import com.tchalanet.server.core.pagemodel.internal.domain.model.PageModelStatus;
import tools.jackson.databind.node.JsonNodeFactory;

final class PageModelMapper {


  static PageModelInstance toDomain(PageModelJpaEntity e) {
    JsonNode node = null;
    try {
      if (e.getModel() != null) node = e.getModel();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }

    PageModelStatus status = PageModelStatus.DRAFT;
    if (e.getStatus() != null) {
      status = PageModelStatus.valueOf(e.getStatus().name());
    }

    return PageModelInstance.rehydrate(
        e.getId(),
        e.getTenantId(),
        e.getLogicalId(),
        e.getScope(),
        e.getSlug(),
        status,
        e.getSchemaVersion() == null ? 1 : e.getSchemaVersion(),
        node,
        e.getTemplateId(),
        e.getCreatedAt(),
        e.getUpdatedAt(),
        e.getCreatedBy(),
        e.getUpdatedBy(),
        e.getPublishedAt(),
        null, // archivedAt not present on entity yet
        null // deletedAt not present on entity yet
    );
  }

  static PageModelJpaEntity toEntity(PageModelInstance d, PageModelJpaEntity e) {
    if (e == null) e = new PageModelJpaEntity();
    e.setId(d.id().value());
    e.setTenantId(d.tenantId().value());
    e.setLogicalId(d.logicalId());
    e.setScope(d.scope());
    e.setCode(d.logicalId() + "-" + d.schemaVersion()); // code is logicalId + schemaVersion for uniqueness
    e.setName(d.logicalId() + "-" + d.schemaVersion()); // code is logicalId + schemaVersion for uniqueness
    e.setSlug(d.slug());
    // [Phase 1] correction: PageStatus inexistant — le type correct est PageModelStatus (analysis §BLOQUANT)
    e.setStatus(d.status());
    e.setSchemaVersion(d.schemaVersion());
    e.setModel(d.modelJson());
    e.setTemplateId(d.templateId().map(PageModelTemplateId::value).orElse(null));

    e.setCreatedAt(d.createdAt());
    e.setUpdatedAt(d.updatedAt());
    e.setCreatedBy(d.createdBy() != null ? d.createdBy().value(): null);
    e.setUpdatedBy(d.updatedBy() != null ? d.updatedBy().value(): null);
    e.setPublishedAt(d.publishedAt().orElse(null));
    e.setSchema(JsonNodeFactory.instance.objectNode());
    // archivedAt / deletedAt not stored on this entity yet
    return e;
  }
}
