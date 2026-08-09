package com.tchalanet.server.core.drawresult.internal.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.core.drawresult.api.model.ResultQuality;
import com.tchalanet.server.core.drawresult.internal.application.port.out.external.ExternalResultItem;
import com.tchalanet.server.core.drawresult.internal.application.port.out.external.ExternalSourceFlags;
import com.tchalanet.server.core.haiti.internal.application.port.out.HaitiLotteryPort;
import com.tchalanet.server.core.haiti.internal.application.port.out.HaitiProjectionConfigPort;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

class HaitiProjectionServiceTest {

  private final HaitiProjectionConfigPort configPort = mock(HaitiProjectionConfigPort.class);
  private final HaitiProjectionService service =
      new HaitiProjectionService(
          mock(HaitiLotteryPort.class),
          configPort,
          new JsonUtils(JsonMapper.builder().build()),
          new SimpleMeterRegistry());

  @Test
  void pick4OnlyProducesLot2AndLot3AndMarksProjectionPartial() {
    var slot = slot();
    when(configPort.resolve(slot.projectionCfg())).thenReturn(null);

    var result =
        service.project(
            slot,
            LocalDate.parse("2026-07-20"),
            ResolvedExternalResults.of(null, pick4("1234"), null));

    assertThat(result.flags().projectionOk()).isFalse();
    assertThat(result.flags().projectionReason()).isEqualTo("PARTIAL_EXTERNAL_PICK");
    assertThat(text(result.haitiResult(), "lot1")).isEmpty();
    assertThat(text(result.haitiResult(), "lot2")).isEqualTo("12");
    assertThat(text(result.haitiResult(), "lot3")).isEqualTo("34");
    assertThat(text(result.haitiResult(), "lot4")).isEmpty();
  }

  @Test
  void pick3OnlyProducesLot1AndLot4AndMarksProjectionPartial() {
    var slot = slot();
    when(configPort.resolve(slot.projectionCfg())).thenReturn(null);

    var result =
        service.project(
            slot,
            LocalDate.parse("2026-07-20"),
            ResolvedExternalResults.of(pick3("987"), null, null));

    assertThat(result.flags().projectionOk()).isFalse();
    assertThat(result.flags().projectionReason()).isEqualTo("PARTIAL_EXTERNAL_PICK");
    assertThat(text(result.haitiResult(), "lot1")).isEqualTo("987");
    assertThat(text(result.haitiResult(), "lot2")).isEmpty();
    assertThat(text(result.haitiResult(), "lot3")).isEmpty();
    assertThat(text(result.haitiResult(), "lot4")).isEqualTo("98");
  }

  @Test
  void pick3OnlyIsOkWhenPick4IsNotExpected() {
    var slot = slot();
    when(configPort.resolve(slot.projectionCfg())).thenReturn(null);

    var result =
        service.project(
            slot,
            LocalDate.parse("2026-07-20"),
            ResolvedExternalResults.of(pick3("865"), null, null),
            new ResultSlotSourceConfig(
                "MIDDAY",
                new ResultSlotSourceConfig.SourceGame("DAILY3", true),
                new ResultSlotSourceConfig.SourceGame("DAILY4", false),
                ResultSlotSourceConfig.TrustPolicy.TRUST_PROVIDER));

    assertThat(result.flags().projectionOk()).isTrue();
    assertThat(text(result.haitiResult(), "lot1")).isEqualTo("865");
    assertThat(text(result.haitiResult(), "lot2")).isEmpty();
    assertThat(text(result.haitiResult(), "lot3")).isEmpty();
    assertThat(text(result.haitiResult(), "lot4")).isEqualTo("86");
  }

  private ResultSlotView slot() {
    return new ResultSlotView(
        ResultSlotId.of(UUID.randomUUID()),
        "NY_MID",
        "NY",
        ZoneId.of("America/New_York"),
        LocalTime.of(14, 30),
        "MON,TUE,WED,THU,FRI,SAT,SUN",
        true,
        JsonUtils.emptyObject(),
        JsonUtils.emptyObject(),
        "draw_channel.ny.mid.label");
  }

  private ExternalResultItem pick3(String value) {
    return item("PICK3", value);
  }

  private ExternalResultItem pick4(String value) {
    return item("PICK4", value);
  }

  private ExternalResultItem item(String gameCode, String value) {
    return new ExternalResultItem(
        gameCode,
        value.chars().mapToObj(c -> String.valueOf((char) c)).toList(),
        List.of(),
        ResultQuality.COMPLETE,
        new ExternalSourceFlags("TEST", gameCode + "-" + value, "https://example.test", Map.of()),
        Instant.parse("2026-07-20T18:30:00Z"),
        null);
  }

  private String text(ObjectNode node, String field) {
    var value = node == null ? null : node.get(field);
    return value == null || value.isNull() ? "" : value.asText();
  }
}
