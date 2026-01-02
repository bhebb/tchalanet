package com.tchalanet.server.common.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
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

  // --- convenience parsing helpers used by external providers ---
  public JsonNode readTree(String json) {
    if (json == null) return null;
    try {
      return objectMapper.readTree(json);
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse JSON", e);
    }
  }

  public <T> T fromJson(String json, Class<T> clazz) {
    if (json == null) return null;
    try {
      return objectMapper.readValue(json, clazz);
    } catch (Exception e) {
      throw new RuntimeException("Failed to deserialize JSON to " + clazz.getName(), e);
    }
  }

  public <T> T fromJson(String json, TypeReference<T> typeRef) {
    if (json == null) return null;
    try {
      return objectMapper.readValue(json, typeRef);
    } catch (Exception e) {
      throw new RuntimeException("Failed to deserialize JSON to TypeReference", e);
    }
  }

  public <T> T convertValue(Object fromValue, TypeReference<T> toTypeRef) {
    if (fromValue == null) return null;
    try {
      return objectMapper.convertValue(fromValue, toTypeRef);
    } catch (Exception e) {
      throw new RuntimeException("Failed to convert value to target type", e);
    }
  }
}
