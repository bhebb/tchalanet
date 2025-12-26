package com.tchalanet.server.common.web.converter;

import com.tchalanet.server.common.types.id.RoleId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToRoleIdConverter implements Converter<String, RoleId> {
  @Override
  public RoleId convert(String source) {
    if (source == null || source.isBlank()) return null;
    return RoleId.of(java.util.UUID.fromString(source));
  }
}
