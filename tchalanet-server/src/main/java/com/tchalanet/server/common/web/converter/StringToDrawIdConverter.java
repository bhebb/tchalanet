package com.tchalanet.server.common.web.converter;

import com.tchalanet.server.common.types.id.DrawId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToDrawIdConverter implements Converter<String, DrawId> {
  @Override
  public DrawId convert(String source) {
    if (source == null || source.isBlank()) return null;
    return DrawId.of(source);
  }
}

