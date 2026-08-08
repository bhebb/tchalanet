package com.tchalanet.server.core.drawresult.internal.infra.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.catalog.drawchannel.api.model.DrawSource;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.core.drawresult.api.model.DrawResultStatus;
import com.tchalanet.server.core.drawresult.api.model.ResultQuality;
import com.tchalanet.server.core.drawresult.api.query.view.DrawResultView;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class DrawResultWebMapperTest {

  private final DrawResultWebMapper mapper = new DrawResultWebMapper();
  private final JsonUtils jsonUtils = new JsonUtils(JsonMapper.builder().build());

  @Test
  void mapsNestedHaitiLotsToNumbers() {
    var response =
        mapper.toResponse(
            view(
                jsonUtils.toJsonNode(
                    java.util.Map.of(
                        "lots",
                        java.util.Map.of(
                            "LOT1", "016", "LOT2", "12", "LOT3", "82", "LOT4", "01")))));

    assertThat(response.numbers()).containsExactly("016", "12", "82", "01");
  }

  @Test
  void keepsFlatHaitiLotsToNumbers() {
    var response =
        mapper.toResponse(
            view(
                jsonUtils.toJsonNode(
                    java.util.Map.of("lot1", "123", "lot2", "45", "lot3", "67", "lot4", "89"))));

    assertThat(response.numbers()).containsExactly("123", "45", "67", "89");
  }

  private DrawResultView view(tools.jackson.databind.JsonNode haitiResult) {
    return new DrawResultView(
        DrawResultId.of(UUID.randomUUID()),
        "TN_MID",
        LocalDate.parse("2026-08-08"),
        Instant.parse("2026-08-08T17:29:00Z"),
        DrawResultStatus.CONFIRMED,
        DrawSource.MANUAL,
        ResultQuality.COMPLETE,
        null,
        Instant.parse("2026-08-08T10:55:00Z"),
        Instant.parse("2026-08-08T10:56:00Z"),
        JsonUtils.emptyObject(),
        haitiResult,
        JsonUtils.emptyObject(),
        null);
  }
}
