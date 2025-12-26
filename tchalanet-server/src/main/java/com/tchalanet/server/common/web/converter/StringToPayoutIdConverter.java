package com.tchalanet.server.common.web.converter;

import com.tchalanet.server.common.types.id.PayoutId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToPayoutIdConverter implements Converter<String, PayoutId> {
  @Override
  public PayoutId convert(String source) {
    if (source == null || source.isBlank()) return null;
    return PayoutId.of(source);
  }
}

