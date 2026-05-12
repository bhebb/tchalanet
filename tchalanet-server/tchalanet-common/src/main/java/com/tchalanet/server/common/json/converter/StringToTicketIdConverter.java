package com.tchalanet.server.common.json.converter;

import com.tchalanet.server.common.types.id.TicketId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToTicketIdConverter implements Converter<String, TicketId> {
  @Override
  public TicketId convert(String source) {
    if (source == null || source.isBlank()) return null;
    return TicketId.parse(source);
  }
}
