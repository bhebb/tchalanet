package com.tchalanet.server.common.json.converter;

import com.tchalanet.server.common.types.id.ResultSlotId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/** Spring MVC Converter: String -> ResultSlotId */
@Component
public class StringToResultSlotIdConverter implements Converter<String, ResultSlotId> {

  @Override
  public ResultSlotId convert(String source) {
    if (source == null || source.isBlank()) return null;
    return ResultSlotId.parse(source);
  }
}
