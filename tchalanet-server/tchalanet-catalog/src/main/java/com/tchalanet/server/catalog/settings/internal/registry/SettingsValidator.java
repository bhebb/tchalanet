package com.tchalanet.server.catalog.settings.internal.registry;

import com.tchalanet.server.catalog.settings.api.model.SettingValueType;
import com.tchalanet.server.common.util.JsonUtilsHolder;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;

/**
 * Settings Validator (INTERNAL)
 *
 * <p>Validates setting keys and values against the registry.
 */
@UtilityClass
public class SettingsValidator {

    /**
     * Validate a setting and throw if invalid.
     *
     * @param namespace setting namespace
     * @param key       setting key
     * @param type      value type
     * @param value     value as text
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateOrThrow(
        String namespace, String key, SettingValueType type, String value) {
        String fullKey = namespace + "." + key;

        // 1. Check if setting exists in registry
        var registry = SettingsRegistry.byFullKey();
        var def = registry.get(fullKey);
        if (def == null) {
            throw new IllegalArgumentException("Unknown setting key: " + fullKey);
        }

        // 2. Check type matches
        if (def.type() != type) {
            throw new IllegalArgumentException(
                "Wrong value type for "
                    + fullKey
                    + ": expected "
                    + def.type()
                    + ", got "
                    + type);
        }

        // 3. Parse value to ensure it's valid
        parseValue(type, value);
    }

    /**
     * Parse and validate a value according to its type.
     *
     * @param type  value type
     * @param value value as text
     * @throws IllegalArgumentException if parsing fails
     */
    private static void parseValue(SettingValueType type, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Setting value cannot be null or blank");
        }

        try {
            switch (type) {
                case BOOLEAN -> Boolean.parseBoolean(value);
                case INT -> Integer.parseInt(value);
                case LONG -> Long.parseLong(value);
                case DECIMAL -> new BigDecimal(value);
                case JSON -> {
                    var mapper = JsonUtilsHolder.get();
                    if (mapper != null) {
                        mapper.toJsonNode(value);
                    }
                }
                case STRING -> {
                    // No parsing required
                }
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException(
                "Invalid value for type " + type + ": " + value, ex);
        }
    }
}
