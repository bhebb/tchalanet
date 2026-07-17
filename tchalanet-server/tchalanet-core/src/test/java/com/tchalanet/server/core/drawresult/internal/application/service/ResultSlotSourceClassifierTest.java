package com.tchalanet.server.core.drawresult.internal.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.core.drawresult.internal.application.port.out.external.ExternalResultProviderCapabilityPort;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;

class ResultSlotSourceClassifierTest {

  @Test
  void inactiveSlotIsInactive() {
    var classifier = classifier(provider -> true);

    assertThat(classifier.classify(slot("NY", false, sourceCfg(true))))
        .isEqualTo(ResultSlotSourceClassification.INACTIVE);
  }

  @Test
  void activeSlotWithoutActiveSourceGamesIsManual() {
    var classifier = classifier(provider -> true);

    assertThat(classifier.classify(slot("NY", true, JsonUtils.emptyObject())))
        .isEqualTo(ResultSlotSourceClassification.MANUAL);
  }

  @Test
  void activeSlotWithNoOpClientIsManualEvenWithSourceConfig() {
    var classifier = classifier(provider -> false);

    assertThat(classifier.classify(slot("MN", true, sourceCfg(true))))
        .isEqualTo(ResultSlotSourceClassification.MANUAL);
  }

  @Test
  void activeSlotWithRealClientAndSourceConfigIsAutomatic() {
    var classifier = classifier(provider -> "FL".equals(provider));

    assertThat(classifier.classify(slot("FL", true, sourceCfg(true))))
        .isEqualTo(ResultSlotSourceClassification.AUTOMATIC);
  }

  @Test
  void sourceConfigPresenceAloneDoesNotMakeUnknownProviderAutomatic() {
    var classifier = classifier(provider -> false);

    assertThat(classifier.classify(slot("UNKNOWN", true, sourceCfg(true))))
        .isEqualTo(ResultSlotSourceClassification.MANUAL);
  }

  private static ResultSlotSourceClassifier classifier(ExternalResultProviderCapabilityPort port) {
    return new ResultSlotSourceClassifier(new ResultSlotSourceConfigResolver(), port);
  }

  private static ResultSlotView slot(String provider, boolean active, ObjectNode sourceCfg) {
    return new ResultSlotView(
        ResultSlotId.of(UUID.randomUUID()),
        provider + "_EVE",
        provider,
        ZoneId.of("America/New_York"),
        LocalTime.of(20, 0),
        "MON-SUN",
        active,
        sourceCfg,
        JsonUtils.emptyObject(),
        "slot." + provider);
  }

  private static ObjectNode sourceCfg(boolean active) {
    var root = JsonUtils.emptyObject();
    root.put("provider_slot_code", "evening");
    var pick3 = root.putObject("pick3");
    pick3.put("game_code", "PICK3");
    pick3.put("active", active);
    return root;
  }
}
