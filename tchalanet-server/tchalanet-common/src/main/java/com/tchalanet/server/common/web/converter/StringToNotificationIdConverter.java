package com.tchalanet.server.common.web.converter;

import com.tchalanet.server.common.types.id.NotificationId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToNotificationIdConverter implements Converter<String, NotificationId> {
  @Override
  public NotificationId convert(String source) {
    if (source == null || source.isBlank()) {
      return null;
    }
    return NotificationId.parse(source);
  }
}
