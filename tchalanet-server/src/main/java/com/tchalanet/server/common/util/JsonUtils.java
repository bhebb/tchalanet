package com.tchalanet.server.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Utilities around Jackson ObjectMapper to centralize exception handling and common operations. */
@Component
@RequiredArgsConstructor
public class JsonUtils {

  private final ObjectMapper mapper;

  public String toJson(Object value) {
    try {
      return mapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize object to JSON", e);
    }
  }

  public <T> T readValue(InputStream is, Class<T> clazz) {
    try {
      return mapper.readValue(is, clazz);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read JSON from InputStream", e);
    }
  }

  public <T> T readValue(String json, Class<T> clazz) {
    try {
      return mapper.readValue(json, clazz);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read JSON from String", e);
    }
  }

  public <T> T readValue(String json, TypeReference<T> typeRef) {
    try {
      return mapper.readValue(json, typeRef);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read JSON from String with TypeReference", e);
    }
  }

  public <T> T treeToValue(JsonNode node, Class<T> clazz) {
    try {
      return mapper.treeToValue(node, clazz);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to convert JsonNode to object", e);
    }
  }

  public <T> T convertValue(Object fromValue, TypeReference<T> toValueTypeRef) {
    return mapper.convertValue(fromValue, toValueTypeRef);
  }

  public JsonNode valueToTree(Object value) {
    return mapper.valueToTree(value);
  }

  public ObjectNode emptyObjectNode() {
    return mapper.createObjectNode();
  }

  public JsonNode parse(String json) {
    try {
      return mapper.readTree(json);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to parse JSON", e);
    }
  }
}
