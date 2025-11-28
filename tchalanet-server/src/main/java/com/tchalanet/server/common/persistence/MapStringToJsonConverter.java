package com.tchalanet.server.common.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Collections;
import java.util.Map;

@Converter(autoApply = false)
public class MapStringToJsonConverter implements AttributeConverter<Map<String, String>, String> {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Override
  public String convertToDatabaseColumn(Map<String, String> attribute) {
    try {
      return MAPPER.writeValueAsString(attribute == null ? Collections.emptyMap() : attribute);
    } catch (Exception e) {
      return "{}";
    }
  }

  @Override
  public Map<String, String> convertToEntityAttribute(String dbData) {
    try {
      if (dbData == null) return Collections.emptyMap();
      return MAPPER.readValue(dbData, new TypeReference<Map<String, String>>() {});
    } catch (Exception e) {
      return Collections.emptyMap();
    }
  }
}
