package com.tchalanet.server.common.web.converter;

import com.tchalanet.server.common.types.id.LimitDefinitionId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToLimitDefinitionIdConverter implements Converter<String, LimitDefinitionId> {
  @Override
  public LimitDefinitionId convert(String source) {
    if (source == null || source.isBlank()) return null;
    return LimitDefinitionId.parse(source);
  }
}
