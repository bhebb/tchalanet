package com.tchalanet.server.features.publicdrawresults;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.drawresult.api.query.view.PublicDrawResultHistoryRowView;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class PublicDrawResultViewMapperTest {

  private final PublicDrawResultViewMapper mapper = new PublicDrawResultViewMapper();

  @Test
  void history_preservesNamedLotsWhenAProviderResultIsIncomplete() throws Exception {
    var haiti =
        JsonMapper.builder()
            .build()
            .readTree("{\"lot1\":\"123\",\"lot2\":null,\"lot3\":null,\"lot4\":\"45\"}");
    var view =
        new PublicDrawResultHistoryRowView(
            "GA_EVE",
            "GA",
            "draw_channel.ga.eve.label",
            "Georgia - Evening",
            "America/New_York",
            LocalTime.of(18, 59),
            LocalDate.of(2026, 7, 19),
            Instant.parse("2026-07-19T22:59:00Z"),
            "PROVISIONAL",
            "SUSPECT",
            haiti,
            null,
            "00000000-0000-0000-0000-000000000001");

    var response =
        mapper.toHistoryResponse(new TchPage<>(List.of(view), 0, 50, 1L, 1, true, false, false));

    var row = response.items().single();
    assertThat(row.numbers()).containsExactly("123", "45");
    assertThat(row.lots())
        .containsEntry("LOT1", "123")
        .containsEntry("LOT2", null)
        .containsEntry("LOT3", null)
        .containsEntry("LOT4", "45");
  }
}
