package com.tchalanet.server.common.json.converter;

import com.tchalanet.server.common.types.id.AddressId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Spring converter for AddressId path parameters.
 * Per typed_ids.md section 8: converts UUID string → AddressId.
 * Usage: @PathVariable AddressId addressId
 */
@Component
public class StringToAddressIdConverter implements Converter<String, AddressId> {

  @Override
  public AddressId convert(String source) {
    return AddressId.parse(source);
  }
}
