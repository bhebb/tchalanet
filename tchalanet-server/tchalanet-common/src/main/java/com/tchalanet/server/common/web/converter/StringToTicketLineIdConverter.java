package com.tchalanet.server.common.web.converter;

import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.TicketLineId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToTicketLineIdConverter implements Converter<String, TicketLineId> {
  @Override
  public TicketLineId convert(String source) {
    if (source == null || source.isBlank()) return null;
    return TicketLineId.parse(source);
  }
}
