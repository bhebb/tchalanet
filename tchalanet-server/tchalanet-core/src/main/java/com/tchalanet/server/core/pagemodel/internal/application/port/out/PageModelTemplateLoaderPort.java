package com.tchalanet.server.core.pagemodel.internal.application.port.out;

import com.tchalanet.server.core.pagemodel.domain.model.PageModelDoc;

public interface PageModelTemplateLoaderPort {
  PageModelDoc loadFromResources(String logicalId);
}

