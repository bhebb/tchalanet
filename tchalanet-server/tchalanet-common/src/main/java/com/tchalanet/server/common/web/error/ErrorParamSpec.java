package com.tchalanet.server.common.web.error;

import java.util.Objects;

/** Declares one public parameter permitted by an {@link ErrorDescriptor}. */
public record ErrorParamSpec(String name, ErrorParamType type) {

  public ErrorParamSpec {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(type, "type");
    if (!name.matches("[a-z][a-zA-Z0-9]*")) {
      throw new IllegalArgumentException("Error parameter name must be lower camel case");
    }
  }

  boolean accepts(Object value) {
    return switch (type) {
      case STRING, DISPLAY_LABEL, DATE, DATE_TIME -> value instanceof String;
      case INTEGER ->
          value instanceof Byte
              || value instanceof Short
              || value instanceof Integer
              || value instanceof Long
              || value instanceof java.math.BigInteger;
      case DECIMAL -> value instanceof Number;
      case BOOLEAN -> value instanceof Boolean;
    };
  }
}
