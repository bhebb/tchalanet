package com.tchalanet.server.common.web.converter;

import com.tchalanet.server.common.types.id.TenantGameId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToTenantGameIdConverter implements Converter<String, TenantGameId> {
  @Override
  public TenantGameId convert(String source) {
    if (source == null || source.isBlank()) return null;
    return TenantGameId.of(java.util.UUID.fromString(source));
  }
}
