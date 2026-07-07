package com.tchalanet.server.catalog.resultslot.internal.write;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.common.json.utils.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class ResultSlotConfigValidatorTest {

    private final JsonUtils json = new JsonUtils(JsonMapper.builder().build());
    private ResultSlotConfigValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ResultSlotConfigValidator();
    }

    @Test
    void validSourceConfigPasses() {
        assertThatNoException().isThrownBy(() -> validator.validateSourceCfg(source("""
            {
              "provider_slot_code": "MIDDAY",
              "pick3": {"game_code": "PICK3", "active": true},
              "pick4": {"game_code": "PICK4", "active": false}
            }
            """)));
    }

    @Test
    void sourceConfigRequiresProviderSlotCode() {
        assertThatThrownBy(() -> validator.validateSourceCfg(source("""
            {"pick3": {"game_code": "PICK3", "active": true}}
            """)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("provider_slot_code");
    }

    @Test
    void sourceConfigRequiresAtLeastOneActiveGame() {
        assertThatThrownBy(() -> validator.validateSourceCfg(source("""
            {
              "provider_slot_code": "MIDDAY",
              "pick3": {"game_code": "PICK3", "active": false},
              "pick4": {"game_code": "PICK4", "active": false}
            }
            """)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at least one");
    }

    @Test
    void sourceConfigRequiresBooleanActiveFlag() {
        assertThatThrownBy(() -> validator.validateSourceCfg(source("""
            {
              "provider_slot_code": "MIDDAY",
              "pick3": {"game_code": "PICK3", "active": "true"}
            }
            """)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("active");
    }

    @Test
    void validProjectionConfigPasses() {
        assertThatNoException().isThrownBy(() -> validator.validateProjectionCfg(source("""
            {
              "version": 1,
              "rule_set": "DEFAULT",
              "rules": {
                "lot1": "PICK3_FULL_3",
                "lot2": "PICK4_FIRST2",
                "lot3": "PICK4_LAST2",
                "lot4": "PICK3_FIRST2"
              }
            }
            """)));
    }

    @Test
    void projectionConfigRejectsMissingLot() {
        assertThatThrownBy(() -> validator.validateProjectionCfg(source("""
            {
              "rules": {
                "lot1": "PICK3_FULL_3",
                "lot2": "PICK4_FIRST2",
                "lot3": "PICK4_LAST2"
              }
            }
            """)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("lot4");
    }

    @Test
    void projectionConfigRejectsUnknownToken() {
        assertThatThrownBy(() -> validator.validateProjectionCfg(source("""
            {
              "rules": {
                "lot1": "PICK3_FULL_3",
                "lot2": "PICK4_FIRST2",
                "lot3": "PICK4_LAST2",
                "lot4": "MAGIC"
              }
            }
            """)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("MAGIC");
    }

    private JsonNode source(String value) {
        return json.parse(value);
    }
}
