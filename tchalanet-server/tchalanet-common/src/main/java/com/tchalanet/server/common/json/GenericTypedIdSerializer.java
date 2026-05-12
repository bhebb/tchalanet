package com.tchalanet.server.common.json;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.lang.reflect.Method;

/**
 * Generic serializer for simple typed-id wrapper objects (Jackson 3 / {@link ValueSerializer}).
 *
 * <p>It tries to call a no-arg {@code value()} method by reflection (common in our ID wrappers). If
 * that returns a {@link Number}, it is written as a JSON number. Otherwise the value is written as
 * a JSON string. {@code null} values produce a JSON null.
 *
 * @param <T> wrapper type
 */
public final class GenericTypedIdSerializer<T> extends ValueSerializer<T> {

  private final Class<T> target;
  private final Method valueMethod;

  public GenericTypedIdSerializer(Class<T> target) {
    this.target = target;
    Method m = null;
    try {
      m = target.getMethod("value");
    } catch (NoSuchMethodException ignored) {
      // no value() method — fall back to toString()
    }
    this.valueMethod = m;
  }

  @Override
  public void serialize(T value, JsonGenerator gen, SerializationContext ctxt) {
    if (value == null) {
      gen.writeNull();
      return;
    }

    try {
      if (valueMethod != null) {
        Object raw = valueMethod.invoke(value);
        if (raw == null) {
          gen.writeNull();
          return;
        }
        if (raw instanceof Number n) {
          if (n instanceof Integer || n instanceof Short || n instanceof Byte) {
            gen.writeNumber(n.intValue());
          } else if (n instanceof Long) {
            gen.writeNumber(n.longValue());
          } else if (n instanceof Float || n instanceof Double) {
            gen.writeNumber(n.doubleValue());
          } else {
            gen.writeNumber(n.toString());
          }
          return;
        }
        gen.writeString(raw.toString());
        return;
      }
    } catch (ReflectiveOperationException ignored) {
      // fall through to toString() fallback
    }

    String s = value.toString();
    if (s == null) {
      gen.writeNull();
    } else {
      gen.writeString(s);
    }
  }
}
