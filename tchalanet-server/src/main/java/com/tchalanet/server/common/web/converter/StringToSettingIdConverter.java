package com.tchalanet.server.common.web.converter;

import com.tchalanet.server.common.types.id.SettingId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Spring MVC Converter: String → SettingId
 *
 * <p>Enables @PathVariable and @RequestParam to accept SettingId directly.
 *
 * <p>Example:
 *
 * <pre>
 * {@code @GetMapping("/{id}")}
 * public ResponseEntity<SettingView> get(@PathVariable SettingId id) { ... }
 * </pre>
 */
@Component
public class StringToSettingIdConverter implements Converter<String, SettingId> {

  @Override
  public SettingId convert(String source) {
    return SettingId.parse(source);
  }
}
