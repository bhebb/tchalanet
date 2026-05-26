package com.tchalanet.server.core.pagemodel.api.dynamic;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.pagemodel.api.model.PageModelDoc;

public interface PageModelDynamicProvider {

  boolean supports(String logicalId, String widgetType, String source);

  Object load(
      PageModelDoc pageModel,
      String widgetId,
      PageModelDoc.WidgetConfig widgetConfig,
      String lang,
      TchRequestContext ctx,
      PageModelResolutionContext resolutionContext
  );

  String providerKey();
}

