package com.tchalanet.server.common.web.converter;

import com.tchalanet.server.common.types.id.UserId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToUserIdConverter implements Converter<String, UserId> {
  @Override
  public UserId convert(String source) {
    if (source == null || source.isBlank()) return null;
    return UserId.of(source);
  }
}
