package com.tchalanet.server.core.draw.internal.infra.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.core.draw.api.query.DrawResultSummary;
import com.tchalanet.server.core.drawresult.api.model.DrawResultStatus;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DrawAdminWebMapperTest {

  private final DrawAdminWebMapper mapper = new DrawAdminWebMapperImpl();
  private static final Instant FETCHED_AT = Instant.parse("2026-08-08T17:31:00Z");

  @Test
  void mapsNestedHaitiLotsFromManualResults() {
    var result =
        new DrawResultSummary(
            DrawResultId.of(UUID.randomUUID()),
            DrawResultStatus.CONFIRMED,
            Instant.parse("2026-08-08T17:29:00Z"),
            FETCHED_AT,
            null,
            Map.of("lots", Map.of("LOT1", "016", "LOT2", "12", "LOT3", "82", "LOT4", "01")));

    var response = mapper.toHaitiDrawResultSummaryReponse(result);

    assertThat(response.fetchedAt()).isEqualTo(FETCHED_AT);
    assertThat(response.lot1()).isEqualTo("016");
    assertThat(response.lot2()).isEqualTo("12");
    assertThat(response.lot3()).isEqualTo("82");
    assertThat(response.lot4()).isEqualTo("01");
  }

  @Test
  void keepsFlatHaitiLotsForExistingResults() {
    var result =
        new DrawResultSummary(
            DrawResultId.of(UUID.randomUUID()),
            DrawResultStatus.CONFIRMED,
            Instant.parse("2026-08-08T17:29:00Z"),
            FETCHED_AT,
            null,
            Map.of("lot1", "123", "lot2", "45", "lot3", "67", "lot4", "89"));

    var response = mapper.toHaitiDrawResultSummaryReponse(result);

    assertThat(response.lot1()).isEqualTo("123");
    assertThat(response.lot2()).isEqualTo("45");
    assertThat(response.lot3()).isEqualTo("67");
    assertThat(response.lot4()).isEqualTo("89");
  }
}
