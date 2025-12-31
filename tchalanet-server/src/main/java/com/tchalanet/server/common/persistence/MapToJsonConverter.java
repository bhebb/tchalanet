package com.tchalanet.server.common.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.common.config.ObjectMapperHolder;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Collections;
import java.util.Map;

@Converter(autoApply = false)
public class MapToJsonConverter implements AttributeConverter<Map<String, Object>, String> {

  private static ObjectMapper mapper() {
    return ObjectMapperHolder.get();
  }

  @Override
  public String convertToDatabaseColumn(Map<String, Object> attribute) {
    try {
      ObjectMapper m = mapper();
      return m.writeValueAsString(attribute == null ? Collections.emptyMap() : attribute);
    } catch (Exception e) {
      return "{}";
    }
  }

  @Override
  public Map<String, Object> convertToEntityAttribute(String dbData) {
    try {
      if (dbData == null) return Collections.emptyMap();
      ObjectMapper m = mapper();
      return m.readValue(dbData, new TypeReference<Map<String, Object>>() {});
    } catch (Exception e) {
      return Collections.emptyMap();
    }
  }
}
