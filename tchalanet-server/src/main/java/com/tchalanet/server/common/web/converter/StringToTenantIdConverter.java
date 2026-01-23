package com.tchalanet.server.common.web.converter;

import com.tchalanet.server.common.types.id.TenantId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToTenantIdConverter implements Converter<String, TenantId> {
  @Override
  public TenantId convert(String source) {
    if (source == null || source.isBlank()) return null;
    return TenantId.parse(source);
  }
}
