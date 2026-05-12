package com.tchalanet.server.common.json.converter;

import com.tchalanet.server.common.types.id.PlanId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Spring MVC converter for PathVariable/RequestParam PlanId.
 * Delegates to PlanId.parse() per typed_ids.md convention.
 */
@Component
public class StringToPlanIdConverter implements Converter<String, PlanId> {

  @Override
  public PlanId convert(String source) {
    return PlanId.parse(source);
  }
}
