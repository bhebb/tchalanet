package com.tchalanet.server.common.web.converter;

import com.tchalanet.server.common.types.id.TerminalId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToTerminalIdConverter implements Converter<String, TerminalId> {
  @Override
  public TerminalId convert(String source) {
    if (source == null || source.isBlank()) return null;
    return TerminalId.of(source);
  }
}

