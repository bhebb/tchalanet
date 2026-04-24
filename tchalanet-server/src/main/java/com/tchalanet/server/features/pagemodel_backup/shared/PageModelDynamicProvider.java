package com.tchalanet.server.features.pagemodel_backup.shared;

import com.tchalanet.server.common.context.TchRequestContext;

public interface PageModelDynamicProvider {

  boolean supports(String logicalId, String widgetType, String source);

  String providerKey();

  Object load(PageModel model, PageModel.WidgetConfig config, String lang, TchRequestContext ctx);
}

