package com.tchalanet.server.common.web.converter;

import com.tchalanet.server.common.types.id.AgentId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToAgentIdConverter implements Converter<String, AgentId> {
  @Override
  public AgentId convert(String source) {
    if (source == null || source.isBlank()) return null;
    return AgentId.of(source);
  }
}

