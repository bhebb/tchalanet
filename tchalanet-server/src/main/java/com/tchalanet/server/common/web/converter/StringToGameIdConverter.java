package com.tchalanet.server.common.web.converter;

import com.tchalanet.server.common.types.id.GameId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToGameIdConverter implements Converter<String, GameId> {
  @Override
  public GameId convert(String source) {
    if (source == null || source.isBlank()) return null;
    return GameId.of(java.util.UUID.fromString(source));
  }
}
