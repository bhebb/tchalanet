package com.tchalanet.server.common.web.converter;

import com.tchalanet.server.common.types.id.OutletId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToOutletIdConverter implements Converter<String, OutletId> {
  @Override
  public OutletId convert(String source) {
    if (source == null || source.isBlank()) return null;
    return OutletId.parse(source);
  }
}
