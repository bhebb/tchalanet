package com.tchalanet.server.common.usecase;

import com.tchalanet.server.common.context.TchRequestContext;
import java.util.Map;

public interface GetI18nConfigUseCase {
  // placeholder minimal API
  String getDefaultLocale();

  Map<String, Object> execute(TchRequestContext context, String lang);
}
