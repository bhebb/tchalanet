package com.tchalanet.server.common.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Helpers for storing/reading JSON values into PostgreSQL {@code jsonb} columns. */
@Component
@RequiredArgsConstructor
public class JsonbUtils {

  private final JsonMapper jsonMapper;

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
    if (value instanceof String s) {
      if (isValidJson(s)) return s;
      return toJson(s);
    }
    return toJson(value);
  }

  public String toJson(Object value) {
    try {
      return jsonMapper.writeValueAsString(value);
    } catch (JacksonException e) {
      throw new IllegalStateException("Failed to serialize value to JSON for jsonb column", e);
    }
  }

  public boolean isValidJson(String candidate) {
    if (candidate == null) return false;
    try {
      JsonNode n = jsonMapper.readTree(candidate);
      return n != null;
    } catch (JacksonException ex) {
      return false;
    }
  }

  public JsonNode readTree(String json) {
    if (json == null) return null;
    try {
      return jsonMapper.readTree(json);
    } catch (JacksonException e) {
      throw new IllegalStateException("Failed to parse JSON", e);
    }
  }

  public <T> T fromJson(String json, Class<T> clazz) {
    if (json == null) return null;
    try {
      return jsonMapper.readValue(json, clazz);
    } catch (JacksonException e) {
      throw new IllegalStateException("Failed to deserialize JSON to " + clazz.getName(), e);
    }
  }

  public <T> T fromJson(String json, TypeReference<T> typeRef) {
    if (json == null) return null;
    try {
      return jsonMapper.readValue(json, typeRef);
    } catch (JacksonException e) {
      throw new IllegalStateException("Failed to deserialize JSON to TypeReference", e);
    }
  }

  public <T> T convertValue(Object fromValue, TypeReference<T> toTypeRef) {
    if (fromValue == null) return null;
    try {
      return jsonMapper.convertValue(fromValue, toTypeRef);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to convert value to target type", e);
    }
  }
}
