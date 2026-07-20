package com.tchalanet.server.features.pos.draws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PosAvailableDrawResponseTest {

  @Test
  void exposesDomainIdentifiersAsPrimitiveStrings() {
    var drawId = UUID.randomUUID();
    var channelId = UUID.randomUUID();
    var slotId = UUID.randomUUID();
    var view =
        new PosAvailableDrawView(
            DrawId.of(drawId),
            DrawChannelId.of(channelId),
            LocalDate.of(2026, 7, 19),
            ResultSlotId.of(slotId),
            "CA_MID",
            "CA_MID",
            "California - Midday",
            List.of("BORLETTE"),
            "OPEN",
            Instant.parse("2026-07-19T17:00:00Z"),
            Instant.parse("2026-07-19T18:00:00Z"),
            LocalDate.of(2026, 7, 19),
            LocalTime.of(13, 0),
            "America/Los_Angeles",
            LocalDate.of(2026, 7, 19),
            LocalTime.of(16, 0),
            "America/Port-au-Prince");

    var response =
        PosAvailableDrawResponse.from(
            view, mock(ResultSlotCatalog.class), ZoneId.of("America/Port-au-Prince"));

    assertThat(response.drawId()).isEqualTo(drawId.toString());
    assertThat(response.drawChannelId()).isEqualTo(channelId.toString());
    assertThat(response.resultSlotId()).isEqualTo(slotId.toString());
  }
}
