package com.tchalanet.server.features.pagemodel.shared.template;

import com.tchalanet.server.features.pagemodel.shared.template.dto.PageModelTemplateRequest;
import com.tchalanet.server.features.pagemodel.shared.template.dto.PageModelTemplateResponse;
import org.springframework.stereotype.Component;

@Component
public class PageModelTemplateMapper {

  public PageModelTemplateEntity toEntity(PageModelTemplateRequest req) {
    if (req == null) return null;
    PageModelTemplateEntity e = new PageModelTemplateEntity();
    e.setTenantId(req.getTenantId());
    e.setLogicalId(req.getLogicalId());
    e.setLabel(req.getLabel());
    e.setDescription(req.getDescription());
    e.setSchemaVersion(req.getSchemaVersion());
    e.setModelJson(req.getModelJson());
    if (req.getIsDefault() != null) e.setDefault(req.getIsDefault());
    if (req.getIsSystem() != null) e.setSystem(req.getIsSystem());
    return e;
  }

  public PageModelTemplateResponse toResponse(PageModelTemplateEntity e) {
    if (e == null) return null;
    return new PageModelTemplateResponse(
        e.getId(),
        e.getTenantId(),
        e.getLogicalId(),
        e.getLabel(),
        e.getDescription(),
        e.getSchemaVersion(),
        e.getModelJson(),
        e.isDefault(),
        e.isSystem(),
        e.getCreatedAt(),
        e.getCreatedBy(),
        e.getUpdatedAt(),
        e.getUpdatedBy(),
        e.getDeletedAt(),
        e.getVersion());
  }
}
