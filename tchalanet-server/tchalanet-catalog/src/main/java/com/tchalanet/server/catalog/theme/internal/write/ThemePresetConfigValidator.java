package com.tchalanet.server.catalog.theme.internal.write;

import com.tchalanet.server.common.json.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.Set;

/**
 * Validates theme preset config JSON structure before persistence.
 * Expected shape: modes[], defaultMode, tokens{light{}, dark{}}, editableTokens[], allowedFonts[].
 */
@Component
@RequiredArgsConstructor
public class ThemePresetConfigValidator {

    private static final Set<String> VALID_MODES = Set.of("light", "dark");

    private final JsonUtils jsonUtils;

    public void validate(String configJson) {
        if (configJson == null || configJson.isBlank()) {
            throw new IllegalArgumentException("theme_preset.config must not be blank");
        }

        JsonNode root;
        try {
            root = jsonUtils.parse(configJson);
        } catch (Exception e) {
            throw new IllegalArgumentException("theme_preset.config is not valid JSON: " + e.getMessage());
        }

        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException("theme_preset.config must be a JSON object");
        }

        requireArray(root, "modes");
        requireString(root, "defaultMode");
        requireObject(root, "tokens");

        var defaultMode = root.path("defaultMode").asString("");
        if (!VALID_MODES.contains(defaultMode.toLowerCase())) {
            throw new IllegalArgumentException(
                "theme_preset.config.defaultMode must be one of " + VALID_MODES + ", got: " + defaultMode);
        }

        var tokens = root.path("tokens");
        if (tokens.isObject()) {
            tokens.properties().forEach(entry -> {
                if (!VALID_MODES.contains(entry.getKey().toLowerCase())) {
                    throw new IllegalArgumentException(
                        "theme_preset.config.tokens contains unknown mode key: '" + entry.getKey() + "'");
                }
                if (!entry.getValue().isObject()) {
                    throw new IllegalArgumentException(
                        "theme_preset.config.tokens." + entry.getKey() + " must be an object");
                }
            });
        }

        if (root.has("editableTokens") && !root.path("editableTokens").isArray()) {
            throw new IllegalArgumentException("theme_preset.config.editableTokens must be an array");
        }
        if (root.has("allowedFonts") && !root.path("allowedFonts").isArray()) {
            throw new IllegalArgumentException("theme_preset.config.allowedFonts must be an array");
        }
    }

    private void requireArray(JsonNode node, String field) {
        if (!node.has(field) || !node.path(field).isArray()) {
            throw new IllegalArgumentException("theme_preset.config." + field + " must be a non-null array");
        }
    }

    private void requireString(JsonNode node, String field) {
        var v = node.path(field);
        if (v.isMissingNode() || v.isNull() || !v.isTextual() || v.asText().isBlank()) {
            throw new IllegalArgumentException("theme_preset.config." + field + " must be a non-blank string");
        }
    }

    private void requireObject(JsonNode node, String field) {
        if (!node.has(field) || !node.path(field).isObject()) {
            throw new IllegalArgumentException("theme_preset.config." + field + " must be a non-null object");
        }
    }
}
