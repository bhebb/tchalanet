package com.tchalanet.server.common.persistence.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.ZoneId;

@Converter(autoApply = true)
public class ZoneIdAttributeConverter implements AttributeConverter<ZoneId, String> {
  @Override
  public String convertToDatabaseColumn(ZoneId attribute) {
    return attribute == null ? null : attribute.getId();
  }

  @Override
  public ZoneId convertToEntityAttribute(String dbData) {
    if (dbData == null) return null;
    try {
      return ZoneId.of(dbData);
    } catch (Exception ex) {
      throw new IllegalArgumentException("Invalid zone id: " + dbData, ex);
    }
  }
}
