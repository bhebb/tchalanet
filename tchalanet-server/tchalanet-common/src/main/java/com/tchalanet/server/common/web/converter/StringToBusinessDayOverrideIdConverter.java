package com.tchalanet.server.common.web.converter;

import com.tchalanet.server.common.types.id.BusinessDayOverrideId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/** Spring MVC Converter: String -> BusinessDayOverrideId */
@Component
public class StringToBusinessDayOverrideIdConverter
    implements Converter<String, BusinessDayOverrideId> {

  @Override
  public BusinessDayOverrideId convert(String source) {
    if (source == null || source.isBlank()) return null;
    return BusinessDayOverrideId.parse(source);
  }
}
