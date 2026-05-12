package com.tchalanet.server.features.pagemodel.dynamic.providers;

import com.tchalanet.server.core.pagemodel.domain.model.PageModelDoc;
import com.tchalanet.server.features.pagemodel.dynamic.PageModelDynamicProvider;
import java.util.List;
import java.util.Map;

abstract class StubPageModelDynamicProvider implements PageModelDynamicProvider {

  private final String source;

  protected StubPageModelDynamicProvider(String source) {
    this.source = source;
  }

  @Override
  public boolean supports(String logicalId, String widgetType, String source) {
    return this.source.equals(source);
  }

  @Override
  public Object load(
      PageModelDoc pageModel,
      String widgetId,
      PageModelDoc.WidgetConfig widgetConfig,
      String lang,
      TchRequestContext ctx) {
    // TODO: replace this composition payload once the owning domain exposes a stable query.
    return Map.of(
        "items", List.of(),
        "status", "not_implemented",
        "source", source);
  }

  @Override
  public String providerKey() {
    return source;
  }
}
