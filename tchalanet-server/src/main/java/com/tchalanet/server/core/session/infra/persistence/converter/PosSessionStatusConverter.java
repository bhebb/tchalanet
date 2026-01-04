package com.tchalanet.server.core.session.infra.persistence.converter;

import com.tchalanet.server.core.session.domain.model.PosSessionStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class PosSessionStatusConverter implements AttributeConverter<PosSessionStatus, String> {

  @Override
  public String convertToDatabaseColumn(PosSessionStatus attribute) {
    if (attribute == null) return null;
    return switch (attribute) {
      case OPENED -> "OPEN";
      case CLOSED -> "CLOSED";
      case SETTLED -> "SETTLED";
    };
  }

  @Override
  public PosSessionStatus convertToEntityAttribute(String dbData) {
    if (dbData == null) return null;
    return switch (dbData) {
      case "OPEN" -> PosSessionStatus.OPENED;
      case "CLOSED" -> PosSessionStatus.CLOSED;
      case "SETTLED" -> PosSessionStatus.SETTLED;
      default -> throw new IllegalArgumentException("Unknown PosSessionStatus db value: " + dbData);
    };
  }
}
