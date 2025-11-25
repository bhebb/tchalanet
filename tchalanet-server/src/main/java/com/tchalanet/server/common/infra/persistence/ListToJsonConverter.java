package com.tchalanet.server.common.infra.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Collections;
import java.util.List;

@Converter(autoApply = false)
public class ListToJsonConverter implements AttributeConverter<List<String>, String> {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Override
  public String convertToDatabaseColumn(List<String> attribute) {
    try {
      return MAPPER.writeValueAsString(attribute == null ? Collections.emptyList() : attribute);
    } catch (Exception e) {
      return "[]";
    }
  }

  @Override
  public List<String> convertToEntityAttribute(String dbData) {
    try {
      if (dbData == null) return Collections.emptyList();
      return MAPPER.readValue(dbData, new TypeReference<List<String>>() {});
    } catch (Exception e) {
      return Collections.emptyList();
    }
  }
}
