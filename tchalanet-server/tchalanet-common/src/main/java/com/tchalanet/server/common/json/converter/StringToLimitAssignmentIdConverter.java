package com.tchalanet.server.common.json.converter;

import com.tchalanet.server.common.types.id.LimitAssignmentId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToLimitAssignmentIdConverter implements Converter<String, LimitAssignmentId> {
  @Override
  public LimitAssignmentId convert(String source) {
    if (source == null || source.isBlank()) return null;
    return LimitAssignmentId.parse(source);
  }
}
