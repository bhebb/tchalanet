package com.tchalanet.server.features.pagemodel.application.port.in;

import com.tchalanet.server.common.context.TchRequestContext;
import java.util.Map;

public interface GetI18nConfigUseCase {
  // placeholder minimal API
  String getDefaultLocale();

  Map<String, Object> execute(TchRequestContext context, String lang);
}
