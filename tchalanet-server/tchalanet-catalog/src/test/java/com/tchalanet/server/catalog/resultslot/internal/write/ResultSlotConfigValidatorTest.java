package com.tchalanet.server.catalog.resultslot.internal.write;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.catalog.resultslot.api.error.ResultSlotErrorCodes;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.web.error.ProblemRestException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
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
    assertThatNoException()
        .isThrownBy(
            () ->
                validator.validateSourceCfg(
                    source(
                        """
            {
              "provider_slot_code": "MIDDAY",
              "pick3": {"game_code": "PICK3", "active": true},
              "pick4": {"game_code": "PICK4", "active": false}
            }
            """)));
  }

  @Test
  void sourceConfigRequiresProviderSlotCode() {
    assertConfigInvalid(
        () ->
            validator.validateSourceCfg(
                source(
                    """
            {"pick3": {"game_code": "PICK3", "active": true}}
            """)));
  }

  @Test
  void sourceConfigRequiresAtLeastOneActiveGame() {
    assertConfigInvalid(
        () ->
            validator.validateSourceCfg(
                source(
                    """
            {
              "provider_slot_code": "MIDDAY",
              "pick3": {"game_code": "PICK3", "active": false},
              "pick4": {"game_code": "PICK4", "active": false}
            }
            """)));
  }

  @Test
  void sourceConfigRequiresBooleanActiveFlag() {
    assertConfigInvalid(
        () ->
            validator.validateSourceCfg(
                source(
                    """
            {
              "provider_slot_code": "MIDDAY",
              "pick3": {"game_code": "PICK3", "active": "true"}
            }
            """)));
  }

  @Test
  void validProjectionConfigPasses() {
    assertThatNoException()
        .isThrownBy(
            () ->
                validator.validateProjectionCfg(
                    source(
                        """
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
    assertConfigInvalid(
        () ->
            validator.validateProjectionCfg(
                source(
                    """
            {
              "rules": {
                "lot1": "PICK3_FULL_3",
                "lot2": "PICK4_FIRST2",
                "lot3": "PICK4_LAST2"
              }
            }
            """)));
  }

  @Test
  void projectionConfigRejectsUnknownToken() {
    assertConfigInvalid(
        () ->
            validator.validateProjectionCfg(
                source(
                    """
            {
              "rules": {
                "lot1": "PICK3_FULL_3",
                "lot2": "PICK4_FIRST2",
                "lot3": "PICK4_LAST2",
                "lot4": "MAGIC"
              }
            }
            """)));
  }

  // Code-first error contract: the wire message stays generic; clients (and this
  // test) rely on the stable `code` property instead of message contents.
  private void assertConfigInvalid(ThrowingCallable call) {
    assertThatThrownBy(call)
        .isInstanceOfSatisfying(
            ProblemRestException.class,
            ex ->
                assertThat(ex.getProblem().getProperties())
                    .containsEntry("code", ResultSlotErrorCodes.CONFIG_INVALID.code()));
  }

  private JsonNode source(String value) {
    return json.parse(value);
  }
}
