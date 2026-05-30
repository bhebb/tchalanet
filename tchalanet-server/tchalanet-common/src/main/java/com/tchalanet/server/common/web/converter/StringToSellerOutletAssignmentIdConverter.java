package com.tchalanet.server.common.web.converter;

import com.tchalanet.server.common.types.id.SellerOutletAssignmentId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToSellerOutletAssignmentIdConverter implements Converter<String, SellerOutletAssignmentId> {
  @Override
  public SellerOutletAssignmentId convert(String source) {
    if (source == null || source.isBlank()) return null;
    return SellerOutletAssignmentId.parse(source);
  }
}
