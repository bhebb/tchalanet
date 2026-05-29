package com.tchalanet.server.common.web.converter;

import com.tchalanet.server.common.types.id.SellerId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToSellerIdConverter implements Converter<String, SellerId> {
  @Override
  public SellerId convert(String source) {
    if (source == null || source.isBlank()) return null;
    return SellerId.parse(source);
  }
}
