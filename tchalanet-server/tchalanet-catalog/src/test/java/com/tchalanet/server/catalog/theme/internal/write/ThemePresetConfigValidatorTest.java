package com.tchalanet.server.catalog.theme.internal.write;

import com.tchalanet.server.common.json.utils.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ThemePresetConfigValidatorTest {

    private ThemePresetConfigValidator validator;

    private static final String VALID_CONFIG = """
        {
          "modes": ["light", "dark"],
          "defaultMode": "light",
          "tokens": {
            "light": { "color.primary": "#6750A4" },
            "dark":  { "color.primary": "#D0BCFF" }
          },
          "editableTokens": ["color.primary"],
          "allowedFonts": ["roboto"]
        }
        """;

    @BeforeEach
    void setUp() {
        validator = new ThemePresetConfigValidator(new JsonUtils(JsonMapper.builder().build()));
    }

    @Test
    void validConfigPassesValidation() {
        assertThatNoException().isThrownBy(() -> validator.validate(VALID_CONFIG));
    }

    @Test
    void nullConfigIsRejected() {
        assertThatThrownBy(() -> validator.validate(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must not be blank");
    }

    @Test
    void blankConfigIsRejected() {
        assertThatThrownBy(() -> validator.validate("  "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must not be blank");
    }

    @Test
    void invalidJsonIsRejected() {
        assertThatThrownBy(() -> validator.validate("{ not valid json }"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not valid JSON");
    }

    @Test
    void missingModesFieldIsRejected() {
        var config = """
            { "defaultMode": "light", "tokens": { "light": {} }, "editableTokens": [] }
            """;
        assertThatThrownBy(() -> validator.validate(config))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("modes");
    }

    @Test
    void missingDefaultModeIsRejected() {
        var config = """
            { "modes": ["light"], "tokens": { "light": {} } }
            """;
        assertThatThrownBy(() -> validator.validate(config))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("defaultMode");
    }

    @Test
    void invalidDefaultModeValueIsRejected() {
        var config = """
            { "modes": ["light"], "defaultMode": "rainbow", "tokens": { "light": {} } }
            """;
        assertThatThrownBy(() -> validator.validate(config))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("rainbow");
    }

    @Test
    void missingTokensFieldIsRejected() {
        var config = """
            { "modes": ["light"], "defaultMode": "light" }
            """;
        assertThatThrownBy(() -> validator.validate(config))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tokens");
    }

    @Test
    void unknownTokensModeKeyIsRejected() {
        var config = """
            { "modes": ["light"], "defaultMode": "light",
              "tokens": { "light": {}, "rainbow": {} } }
            """;
        assertThatThrownBy(() -> validator.validate(config))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("rainbow");
    }

    @Test
    void editableTokensNotArrayIsRejected() {
        var config = """
            { "modes": ["light"], "defaultMode": "light",
              "tokens": { "light": {} }, "editableTokens": "color.primary" }
            """;
        assertThatThrownBy(() -> validator.validate(config))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("editableTokens");
    }

    @Test
    void allowedFontsNotArrayIsRejected() {
        var config = """
            { "modes": ["light"], "defaultMode": "light",
              "tokens": { "light": {} }, "allowedFonts": "roboto" }
            """;
        assertThatThrownBy(() -> validator.validate(config))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("allowedFonts");
    }
}
