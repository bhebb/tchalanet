package com.tchalanet.server.common.web.converter;

import com.tchalanet.server.common.types.id.ResultSlotCalendarOverrideId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/** Spring MVC Converter: String -> ResultSlotCalendarOverrideId */
@Component
public class StringToResultSlotCalendarOverrideIdConverter
    implements Converter<String, ResultSlotCalendarOverrideId> {

  @Override
  public ResultSlotCalendarOverrideId convert(String source) {
    if (source == null || source.isBlank()) return null;
    return ResultSlotCalendarOverrideId.parse(source);
  }
}
