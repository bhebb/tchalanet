package com.tchalanet.server.core.pagemodel.application.query.handler;

import com.tchalanet.server.catalog.pagemodeltemplate.api.PageModelTemplateCatalog;
import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.pagemodel.application.port.out.PageModelReadPort;
import com.tchalanet.server.core.pagemodel.application.query.model.PreviewTemplateUpdateQuery;
import com.tchalanet.server.core.pagemodel.application.query.model.TemplateUpdateCompatibility;
import com.tchalanet.server.core.pagemodel.application.query.model.TemplateUpdateDiffView;
import com.tchalanet.server.core.pagemodel.application.query.model.TemplateUpdatePreviewView;
import com.tchalanet.server.core.pagemodel.application.query.model.TemplateUpdateRecommendedAction;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class PreviewTemplateUpdateQueryHandler
    implements QueryHandler<PreviewTemplateUpdateQuery, TemplateUpdatePreviewView> {

  private final PageModelTemplateCatalog templates;
  private final PageModelReadPort pageModels;

  @Override
  public TemplateUpdatePreviewView handle(PreviewTemplateUpdateQuery query) {
    var template =
        templates
            .findByLogicalId(query.logicalId())
            .orElseThrow(() -> new IllegalArgumentException("page_model_template.not_found"));
    var current = pageModels.findPublishedByLogicalId(query.logicalId()).orElse(null);
    var currentVersion = current == null ? 0 : current.schemaVersion();
    var templateVersion = template.schemaVersion() == null ? 1 : template.schemaVersion();
    var compatibility = classify(currentVersion, templateVersion);
    var modelChanged = current != null && !template.model().equals(current.modelJson());
    var conflicts =
        compatibility == TemplateUpdateCompatibility.MAJOR
            ? List.of("schema_version")
            : List.<String>of();
    var changed = modelChanged ? List.of("model") : List.<String>of();
    var recommended =
        compatibility == TemplateUpdateCompatibility.MAJOR
            ? TemplateUpdateRecommendedAction.REQUIRES_MIGRATION
            : conflicts.isEmpty()
                ? TemplateUpdateRecommendedAction.CREATE_DRAFT
                : TemplateUpdateRecommendedAction.MERGE_WITH_CONFLICTS;
    return new TemplateUpdatePreviewView(
        template.id(),
        query.logicalId(),
        currentVersion,
        templateVersion,
        false,
        compatibility,
        recommended,
        new TemplateUpdateDiffView(List.of(), List.of(), changed, conflicts));
  }

  static TemplateUpdateCompatibility classify(int currentVersion, int templateVersion) {
    if (templateVersion <= currentVersion) {
      return TemplateUpdateCompatibility.PATCH;
    }
    if (templateVersion == currentVersion + 1) {
      return TemplateUpdateCompatibility.MINOR;
    }
    return TemplateUpdateCompatibility.MAJOR;
  }
}
