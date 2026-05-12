package com.tchalanet.server.common.json;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.exc.InvalidFormatException;

import java.lang.reflect.Method;

/**
 * Lightweight generic deserializer for typed-id wrappers exposing a static {@code parse(String)}
 * method (Jackson 3 / {@link ValueDeserializer}). Accepts either a JSON string ("uuid-string") or
 * an object {@code {"value":"uuid-string"}}.
 */
public class GenericTypedIdDeserializer<T> extends ValueDeserializer<T> {

  private final Class<T> target;
  private final Method parseMethod;

  public GenericTypedIdDeserializer(Class<T> target) {
    this.target = target;
    Method m = null;
    try {
      m = target.getMethod("parse", String.class);
    } catch (NoSuchMethodException ignored) {
      // parse method not found — we'll fail later if needed
    }
    this.parseMethod = m;
  }

  @Override
  @SuppressWarnings("unchecked")
  public T deserialize(JsonParser p, DeserializationContext ctxt) {
    if (p.currentToken() == JsonToken.VALUE_NULL) return null;

    String str = null;
    if (p.currentToken() == JsonToken.VALUE_STRING) {
      str = p.getString();
    } else {
      JsonNode node = ctxt.readTree(p);
      if (node == null || node.isNull()) return null;
      JsonNode v = node.get("value");
      if (v != null && v.isTextual()) {
        str = v.asString();
      } else if (node.isTextual()) {
        str = node.asString();
      }
    }

    if (str == null || str.isBlank()) return null;

    if (parseMethod != null) {
      try {
        return (T) parseMethod.invoke(null, str.trim());
      } catch (ReflectiveOperationException ex) {
        throw InvalidFormatException.from(
            p, "Failed to parse " + target.getSimpleName() + " from '" + str + "'", str, target);
      }
    }

    throw InvalidFormatException.from(
        p, "No parse(String) method found on " + target.getName(), str, target);
  }
}
