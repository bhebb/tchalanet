package com.tchalanet.server.common.persistence.converter;

import com.tchalanet.server.common.json.utils.JsonUtilsHolder;
import tools.jackson.core.type.TypeReference;
import com.tchalanet.server.common.json.utils.JsonUtils;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Collections;
import java.util.List;

@Converter
public class ListToJsonConverter implements AttributeConverter<List<String>, String> {

  // use JsonUtils (spring-managed) via holder pattern to keep AttributeConverter simple
  private static JsonUtils json() {
    return JsonUtilsHolder.get();
  }

  @Override
  public String convertToDatabaseColumn(List<String> attribute) {
    try {
      return json().toJson(attribute == null ? Collections.emptyList() : attribute);
    } catch (Exception e) {
      return "[]";
    }
  }

  @Override
  public List<String> convertToEntityAttribute(String dbData) {
    try {
      if (dbData == null) return Collections.emptyList();
      return json().readValue(dbData, new TypeReference<>() {});
    } catch (Exception e) {
      return Collections.emptyList();
    }
  }
}
