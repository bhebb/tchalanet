package com.tchalanet.server.core.pagemodel.infra.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.core.pagemodel.domain.model.PageModelInstance;
import com.tchalanet.server.core.pagemodel.domain.model.PageModelStatus;
import com.tchalanet.server.features.pagemodel.PageStatus;

final class PageModelMapper {

  private static final ObjectMapper M = new ObjectMapper();

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
    e.setId(d.id());
    e.setTenantId(d.tenantId());
    e.setLogicalId(d.logicalId());
    e.setScope(d.scope());
    e.setSlug(d.slug());
    // convert domain status -> feature PageStatus
    e.setStatus(PageStatus.valueOf(d.status().name()));
    e.setSchemaVersion(d.schemaVersion());
    e.setModel(d.modelJson());
    e.setTemplateId(d.templateId().orElse(null));

    e.setCreatedAt(d.createdAt());
    e.setUpdatedAt(d.updatedAt());
    e.setCreatedBy(d.createdBy());
    e.setUpdatedBy(d.updatedBy());
    e.setPublishedAt(d.publishedAt().orElse(null));
    // archivedAt / deletedAt not stored on this entity yet
    return e;
  }
}
