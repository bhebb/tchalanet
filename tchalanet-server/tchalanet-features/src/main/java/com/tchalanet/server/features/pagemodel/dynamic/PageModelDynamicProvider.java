package com.tchalanet.server.features.pagemodel.dynamic;

import com.tchalanet.server.common.context.TchRequestContext;

import com.tchalanet.server.core.pagemodel.internal.domain.model.PageModelDoc;

public interface PageModelDynamicProvider {

  boolean supports(String logicalId, String widgetType, String source);

  Object load(
      PageModelDoc pageModel,
      String widgetId,
      PageModelDoc.WidgetConfig widgetConfig,
      String lang,
      TchRequestContext ctx
  );

  String providerKey();
}

