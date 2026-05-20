package com.tchalanet.server.common.persistence.converter;

import com.tchalanet.server.common.json.utils.JsonUtilsHolder;
import tools.jackson.core.type.TypeReference;
import com.tchalanet.server.common.json.utils.JsonUtils;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Collections;
import java.util.Map;

@Converter
public class MapToJsonConverter implements AttributeConverter<Map<String, Object>, String> {

  private static JsonUtils json() {
    return JsonUtilsHolder.get();
  }

  @Override
  public String convertToDatabaseColumn(Map<String, Object> attribute) {
    try {
      return json().toJson(attribute == null ? Collections.emptyMap() : attribute);
    } catch (Exception e) {
      return "{}";
    }
  }

  @Override
  public Map<String, Object> convertToEntityAttribute(String dbData) {
    try {
      if (dbData == null) return Collections.emptyMap();
      return json().readValue(dbData, new TypeReference<>() {});
    } catch (Exception e) {
      return Collections.emptyMap();
    }
  }
}
