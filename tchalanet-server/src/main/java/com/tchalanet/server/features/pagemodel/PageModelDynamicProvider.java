package com.tchalanet.server.features.pagemodel;

import com.tchalanet.server.common.context.TchRequestContext;

public interface PageModelDynamicProvider {

  boolean supports(String logicalId, String widgetType, String source);

  Object load(
      PageModel pageModel,
      String widgetId,
      PageModel.WidgetConfig widgetConfig,
      String lang,
      TchRequestContext ctx
  );

  String providerKey();
}
