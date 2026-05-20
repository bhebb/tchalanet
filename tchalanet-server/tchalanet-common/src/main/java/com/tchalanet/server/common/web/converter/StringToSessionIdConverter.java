package com.tchalanet.server.common.web.converter;

import com.tchalanet.server.common.types.id.SalesSessionId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToSessionIdConverter implements Converter<String, SalesSessionId> {
  @Override
  public SalesSessionId convert(String source) {
    if (source == null || source.isBlank()) return null;
    return SalesSessionId.parse(source);
  }
}
