package com.tchalanet.server.common.json.converter;

import com.tchalanet.server.common.types.id.I18nOverrideId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Spring MVC Converter: String → I18nOverrideId
 *
 * <p>Enables @PathVariable and @RequestParam to accept I18nOverrideId directly.
 */
@Component
public class StringToI18nOverrideIdConverter implements Converter<String, I18nOverrideId> {

  @Override
  public I18nOverrideId convert(String source) {
    return I18nOverrideId.parse(source);
  }
}
