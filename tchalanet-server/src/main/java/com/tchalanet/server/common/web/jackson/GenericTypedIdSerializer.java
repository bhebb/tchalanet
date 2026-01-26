package com.tchalanet.server.common.web.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * Generic serializer for simple typed-id wrapper objects.
 *
 * It will try to call a no-arg {@code value()} method by reflection (common in ID wrappers).
 * If that returns a Number, it will be written as a JSON number. Otherwise it will be written
 * as a JSON string. If the value or reflection result is null, a JSON null is written.
 *
 * @param <T> wrapper type
 */
public final class GenericTypedIdSerializer<T> extends JsonSerializer<T> {

    private final Class<T> target;
    private final Method valueMethod;

    public GenericTypedIdSerializer(Class<T> target) {
        this.target = target;
        Method m = null;
        try {
            m = target.getMethod("value"); // common in your wrappers
        } catch (NoSuchMethodException ignored) {
            // no value() method, we'll fallback to toString()
        }
        this.valueMethod = m;
    }

    @Override
    public void serialize(T value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
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
                if (raw instanceof Number) {
                    // write as JSON number to preserve numeric type
                    Number n = (Number) raw;
                    // use appropriate JsonGenerator method
                    if (n instanceof Integer || n instanceof Short || n instanceof Byte) {
                        gen.writeNumber(n.intValue());
                    } else if (n instanceof Long) {
                        gen.writeNumber(n.longValue());
                    } else if (n instanceof Float || n instanceof Double) {
                        gen.writeNumber(n.doubleValue());
                    } else {
                        // fallback for BigInteger/BigDecimal and other Number implementations
                        gen.writeNumber(n.toString());
                    }
                    return;
                }
                // non-number -> write as string
                gen.writeString(raw.toString());
                return;
            }
        } catch (Exception e) {
            // fallthrough to fallback
        }

        // fallback: use toString()
        String s = value.toString();
        if (s == null) {
            gen.writeNull();
        } else {
            gen.writeString(s);
        }
    }
}
