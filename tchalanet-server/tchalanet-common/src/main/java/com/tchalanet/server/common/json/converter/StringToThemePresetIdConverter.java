package com.tchalanet.server.common.json.converter;

import com.tchalanet.server.common.types.id.ThemePresetId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToThemePresetIdConverter implements Converter<String, ThemePresetId> {
  @Override
  public ThemePresetId convert(String source) {
    if (source == null || source.isBlank()) return null;
    return ThemePresetId.parse(source);
  }
}
