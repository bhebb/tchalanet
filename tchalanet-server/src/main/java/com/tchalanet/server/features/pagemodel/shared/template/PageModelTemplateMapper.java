package com.tchalanet.server.features.pagemodel.shared.template;

import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.features.pagemodel.shared.template.dto.PageModelTemplateRequest;
import com.tchalanet.server.features.pagemodel.shared.template.dto.PageModelTemplateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PageModelTemplateMapper {

  private final JsonUtils jsonUtils;

  public PageModelTemplateEntity toEntity(PageModelTemplateRequest req) {
    if (req == null) return null;
    PageModelTemplateEntity e = new PageModelTemplateEntity();
    e.setTenantId(req.getTenantId());
    e.setLogicalId(req.getLogicalId());
    e.setLabel(req.getLabel());
    e.setDescription(req.getDescription());
    e.setSchemaVersion(req.getSchemaVersion());
    try {
      e.setModel(jsonUtils.parse(req.getModelJson()));
    } catch (Exception ex) {
      log.error("Unable to parse model JSON", ex);
    }
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
        getModeledJson(e.getModel()),
        e.isDefault(),
        e.isSystem(),
        e.getCreatedAt(),
        e.getCreatedBy(),
        e.getUpdatedAt(),
        e.getUpdatedBy(),
        e.getDeletedAt(),
        e.getVersion());
  }

  private String getModeledJson(com.fasterxml.jackson.databind.JsonNode model) {
    try {
      return jsonUtils.toJson(model);
    } catch (Exception e) {
      log.error("Unable to convert model JSON to PageModel object", e);
      return null;
    }
  }
}
