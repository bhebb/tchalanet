package com.tchalanet.server.common.json.converter;

import com.tchalanet.server.common.types.id.PageModelTemplateId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToPageModelTemplateIdConverter implements Converter<String, PageModelTemplateId> {
  @Override
  public PageModelTemplateId convert(String source) {
    return PageModelTemplateId.parse(source);
  }
}
