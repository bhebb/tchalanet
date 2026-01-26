package com.tchalanet.server.common.web.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * Lightweight generic deserializer for typed id wrappers that expose a static parse(String) method.
 * Accepts either a JSON string ("uuid-string") or an object {"value":"uuid-string"}.
 */
public class GenericTypedIdDeserializer<T> extends JsonDeserializer<T> {
  private final Class<T> target;
  private final Method parseMethod;

  public GenericTypedIdDeserializer(Class<T> target) {
    this.target = target;
    Method m = null;
    try {
      m = target.getMethod("parse", String.class);
    } catch (NoSuchMethodException e) {
      // parse method not found — we'll fail later if needed
    }
    this.parseMethod = m;
  }

  @Override
  @SuppressWarnings("unchecked")
  public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    if (p.getCurrentToken() == JsonToken.VALUE_NULL) return null;

    String str = null;
    if (p.getCurrentToken() == JsonToken.VALUE_STRING) {
      str = p.getText();
    } else {
      JsonNode node = p.readValueAsTree();
      if (node == null || node.isNull()) return null;
      JsonNode v = node.get("value");
      if (v != null && v.isTextual()) {
        str = v.asText();
      } else if (node.isTextual()) {
        str = node.asText();
      }
    }

    if (str == null || str.isBlank()) return null;

    if (parseMethod != null) {
      try {
        return (T) parseMethod.invoke(null, str.trim());
      } catch (Exception ex) {
        throw new IOException("Failed to parse " + target.getSimpleName() + " from '" + str + "'", ex);
      }
    }

    // No parse method available — try to find static of(UUID) or constructor is more complex; prefer explicit deserializer instead.
    throw new IOException("No parse(String) method found on " + target.getName());
  }
}
