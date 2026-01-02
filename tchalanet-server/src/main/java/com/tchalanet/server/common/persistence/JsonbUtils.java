package com.tchalanet.server.common.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JsonbUtils {

  private final ObjectMapper objectMapper;

  public String toJsonOrEmptyArray(Object value) {
    if (value == null) return "[]";
    return toJson(value);
  }

  public String toJsonOrNull(Object value) {
    if (value == null) return null;
    return toJson(value);
  }

  public String toJsonOrEmptyObject(Object value) {
    if (value == null) return "{}";
    // If it's already a JSON string, keep it
    if (value instanceof String s) {
      if (isValidJson(s)) return s;
      // otherwise wrap as JSON string
      return toJson(s);
    }
    return toJson(value);
  }

  public String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to serialize value to JSON for jsonb column", e);
    }
  }

  public boolean isValidJson(String candidate) {
    if (candidate == null) return false;
    try {
      JsonNode n = objectMapper.readTree(candidate);
      return n != null;
    } catch (Exception ex) {
      return false;
    }
  }
}
