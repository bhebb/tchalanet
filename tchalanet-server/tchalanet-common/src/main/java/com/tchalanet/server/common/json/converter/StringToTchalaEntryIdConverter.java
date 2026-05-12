package com.tchalanet.server.common.json.converter;

import com.tchalanet.server.common.types.id.TchalaEntryId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Spring converter to allow binding path variables directly to `TchalaEntryId`.
 * Accepts UUID string representation and delegates to TchalaEntryId.parse().
 */
@Component
public class StringToTchalaEntryIdConverter implements Converter<String, TchalaEntryId> {

  @Override
  public TchalaEntryId convert(String source) {
    if (source == null || source.isBlank()) return null;
    return TchalaEntryId.parse(source);
  }
}
