package com.tchalanet.server.common.web.converter;

import com.tchalanet.server.common.types.id.DrawChannelGameId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToDrawChannelGameIdConverter implements Converter<String, DrawChannelGameId> {
  @Override
  public DrawChannelGameId convert(String source) {
    return DrawChannelGameId.parse(source);
  }
}
