package com.tchalanet.server.common.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.common.config.ObjectMapperHolder;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Collections;
import java.util.List;

@Converter(autoApply = false)
public class ListToJsonConverter implements AttributeConverter<List<String>, String> {

  private static ObjectMapper mapper() {
    return ObjectMapperHolder.get();
  }

  @Override
  public String convertToDatabaseColumn(List<String> attribute) {
    try {
      ObjectMapper m = mapper();
      return m.writeValueAsString(attribute == null ? Collections.emptyList() : attribute);
    } catch (Exception e) {
      return "[]";
    }
  }

  @Override
  public List<String> convertToEntityAttribute(String dbData) {
    try {
      if (dbData == null) return Collections.emptyList();
      ObjectMapper m = mapper();
      return m.readValue(dbData, new TypeReference<List<String>>() {});
    } catch (Exception e) {
      return Collections.emptyList();
    }
  }
}
