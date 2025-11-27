package com.tchalanet.server.draw.infra.persistence.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Converter
@RequiredArgsConstructor
@Slf4j
public class JsonMapConverter implements AttributeConverter<Map<String, Object>, String> {

  private final ObjectMapper objectMapper; // Injected by Spring

  @Override
  public String convertToDatabaseColumn(Map<String, Object> attribute) {
    if (attribute == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(attribute);
    } catch (JsonProcessingException e) {
      log.error("Error converting map to JSON string", e);
      return null; // Or throw a runtime exception
    }
  }

  @Override
  public Map<String, Object> convertToEntityAttribute(String dbData) {
    if (dbData == null) {
      return null;
    }
    try {
      return objectMapper.readValue(dbData, Map.class);
    } catch (IOException e) {
      log.error("Error converting JSON string to map", e);
      return null; // Or throw a runtime exception
    }
  }
}
