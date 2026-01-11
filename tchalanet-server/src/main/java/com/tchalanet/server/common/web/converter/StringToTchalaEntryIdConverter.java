package com.tchalanet.server.common.web.converter;

import com.tchalanet.server.common.types.id.TchalaEntryId;
import java.util.UUID;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Spring converter to allow binding path variables directly to `TchalaEntryId`. Accepts UUID string
 * representation and returns TchalaEntryId.of(uuid).
 */
@Component
public class StringToTchalaEntryIdConverter implements Converter<String, TchalaEntryId> {

  @Override
  public TchalaEntryId convert(String source) {
    if (source == null || source.isBlank()) return null;
    UUID u = UUID.fromString(source.trim());
    return TchalaEntryId.of(u);
  }
}
