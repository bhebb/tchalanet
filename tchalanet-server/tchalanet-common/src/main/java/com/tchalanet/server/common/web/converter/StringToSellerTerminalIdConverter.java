package com.tchalanet.server.common.web.converter;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToSellerTerminalIdConverter implements Converter<String, SellerTerminalId> {
  @Override
  public SellerTerminalId convert(String source) {
    if (source == null || source.isBlank()) {
      return null;
    }
    return SellerTerminalId.parse(source);
  }
}
