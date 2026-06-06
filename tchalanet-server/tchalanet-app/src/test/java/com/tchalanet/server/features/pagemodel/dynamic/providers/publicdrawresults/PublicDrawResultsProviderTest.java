package com.tchalanet.server.features.pagemodel.dynamic.providers.publicdrawresults;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.core.drawresult.api.query.view.PublicDrawResultSlotView;
import com.tchalanet.server.core.drawresult.api.query.view.PublicDrawResultView;
import com.tchalanet.server.core.drawresult.api.query.view.PublicNextResultTimeView;
import com.tchalanet.server.core.pagemodel.api.model.PageModelDoc;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class PublicDrawResultsProviderTest {

  private final JsonUtils jsonUtils = new JsonUtils(JsonMapper.builder().build());

  @Test
  void projectsHomeSlotsWithoutProviderInternals() {
    var slot =
        new PublicDrawResultSlotView(
            "NY_MIDDAY",
            "NY",
            "New York Midday",
            "America/New_York",
            LocalTime.NOON,
            true,
            new PublicNextResultTimeView(
                Instant.parse("2026-06-07T16:00:00Z"),
                LocalDate.parse("2026-06-07"),
                LocalTime.NOON,
                "America/New_York",
                120,
                "WAITING"),
            new PublicDrawResultView(
                LocalDate.parse("2026-06-06"),
                Instant.parse("2026-06-06T16:00:00Z"),
                "FINAL",
                "COMPLETE",
                jsonUtils.toJsonNode(
                    java.util.Map.of("lot1", "123", "lot2", "45", "lot3", "67", "lot4", "89")),
                jsonUtils.toJsonNode(
                    java.util.Map.of("sourceHash", "secret", "sourceFlags", java.util.Map.of()))),
            List.of());

    String json = jsonUtils.toJson(PublicDrawResultsProvider.homeSlots(List.of(slot)));

    assertThat(json)
        .contains("\"slotKey\":\"NY_MIDDAY\"")
        .contains("\"lot1\":\"123\"")
        .contains("\"countdownSeconds\":120")
        .doesNotContain("source")
        .doesNotContain("history")
        .doesNotContain("active")
        .doesNotContain("localDate");
  }

  @Test
  void limitsHomeSlotsFromCamelCaseMaxSlotsProp() {
    var slot =
        new PublicDrawResultSlotView(
            "NY_MIDDAY", "NY", "New York Midday", "America/New_York", LocalTime.NOON, true,
            null, null, List.of());

    assertThat(PublicDrawResultsProvider.homeSlots(List.of(slot, slot), 1)).hasSize(1);
  }

  @Test
  void readsCamelCaseDrawProps() {
    var config =
        new PageModelDoc.WidgetConfig(
            "PublicDrawResultsWidget",
            null,
            Map.of(
                "slotKeys", List.of("NY_MIDDAY"),
                "provider", "NY",
                "includeHistory", true,
                "historyLimit", 7));

    var spec = PublicDrawResultsProvider.buildSpec(config);

    assertThat(spec.slotKeys()).containsExactly("NY_MIDDAY");
    assertThat(spec.provider()).isEqualTo("NY");
    assertThat(spec.includeHistory()).isTrue();
    assertThat(spec.historyLimit()).isEqualTo(7);
  }
}
