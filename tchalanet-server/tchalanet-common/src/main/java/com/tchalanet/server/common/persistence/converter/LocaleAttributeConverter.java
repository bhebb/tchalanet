package com.tchalanet.server.common.persistence.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Locale;

@Converter(autoApply = true)
public class LocaleAttributeConverter implements AttributeConverter<Locale, String> {
  @Override
  public String convertToDatabaseColumn(Locale attribute) {
    return attribute == null ? null : attribute.toLanguageTag();
  }

  @Override
  public Locale convertToEntityAttribute(String dbData) {
    return dbData == null ? null : Locale.forLanguageTag(dbData);
  }
}
