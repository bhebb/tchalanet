package com.tchalanet.server.core.session.infra.persistence.converter;

import com.tchalanet.server.core.session.domain.model.SalesSessionStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class SalesSessionStatusConverter implements AttributeConverter<SalesSessionStatus, String> {

  @Override
  public String convertToDatabaseColumn(SalesSessionStatus attribute) {
    if (attribute == null) return null;
    return switch (attribute) {
      case OPENED -> "OPEN";
      case CLOSED -> "CLOSED";
      case SETTLED -> "SETTLED";
    };
  }

  @Override
  public SalesSessionStatus convertToEntityAttribute(String dbData) {
    if (dbData == null) return null;
    return switch (dbData) {
      case "OPEN" -> SalesSessionStatus.OPENED;
      case "CLOSED" -> SalesSessionStatus.CLOSED;
      case "SETTLED" -> SalesSessionStatus.SETTLED;
      default -> throw new IllegalArgumentException("Unknown SalesSessionStatus db value: " + dbData);
    };
  }
}
