package com.tchalanet.server.common.json.converter;

import com.tchalanet.server.common.types.id.PricingOddsId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/** Spring MVC Converter: String -> PricingOddsId */
@Component
public class StringToPricingOddsIdConverter implements Converter<String, PricingOddsId> {

  @Override
  public PricingOddsId convert(String source) {
    if (source == null || source.isBlank()) return null;
    return PricingOddsId.parse(source);
  }
}
