package com.tchalanet.server.common.web.converter;

import com.tchalanet.server.common.types.id.DrawChannelId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToDrawChannelIdConverter implements Converter<String, DrawChannelId> {
  @Override
  public DrawChannelId convert(String source) {
    return DrawChannelId.parse(source);
  }
}
