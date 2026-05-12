package com.tchalanet.server.core.pagemodel.internal.application.service;

import com.tchalanet.server.catalog.pagemodeltemplate.api.PageModelTemplateCatalog;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.pagemodel.application.port.out.PageModelWritePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PageModelTemplateUpdateActionService {

  private final PageModelTemplateCatalog templates;
  private final PageModelWritePort writer;

  public void applyTemplate(String logicalId, UserId actorId) {
    var template =
        templates
            .findByLogicalId(logicalId)
            .orElseThrow(() -> new IllegalArgumentException("page_model_template.not_found"));
    writer.applyTemplateUpdate(
        template.id(),
        logicalId,
        template.model(),
        template.schemaVersion() == null ? 1 : template.schemaVersion(),
        actorId);
  }
}
