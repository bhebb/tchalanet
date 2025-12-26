package com.tchalanet.server.common.web.converter;

import com.tchalanet.server.common.types.id.SessionId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToSessionIdConverter implements Converter<String, SessionId> {
  @Override
  public SessionId convert(String source) {
    if (source == null || source.isBlank()) return null;
    return SessionId.of(source);
  }
}

