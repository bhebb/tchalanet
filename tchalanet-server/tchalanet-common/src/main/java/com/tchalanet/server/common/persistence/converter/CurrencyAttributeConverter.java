package com.tchalanet.server.common.persistence.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Currency;

@Converter(autoApply = true)
public class CurrencyAttributeConverter implements AttributeConverter<Currency, String> {
  @Override
  public String convertToDatabaseColumn(Currency attribute) {
    return attribute == null ? null : attribute.getCurrencyCode();
  }

  @Override
  public Currency convertToEntityAttribute(String dbData) {
    if (dbData == null) return null;
    try {
      return Currency.getInstance(dbData);
    } catch (Exception ex) {
      throw new IllegalArgumentException("Invalid currency code: " + dbData, ex);
    }
  }
}
