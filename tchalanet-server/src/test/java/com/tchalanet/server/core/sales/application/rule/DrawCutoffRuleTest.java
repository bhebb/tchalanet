package com.tchalanet.server.core.sales.application.rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.error.ProblemRestException;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.application.query.projection.DrawSummary;
import com.tchalanet.server.core.draw.domain.model.DrawStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DrawCutoffRuleTest {

  private static final Instant NOW = Instant.parse("2026-05-06T09:30:00Z");

  @Test
  void allowsOpenDrawBeforeCutoff() {
    var rule = ruleReturning(summary(DrawStatus.OPEN, NOW.plusSeconds(1)));

    var draw = rule.requireBeforeCutoff(DrawId.of(UUID.randomUUID()));

    assertThat(draw.status()).isEqualTo(DrawStatus.OPEN);
  }

  @Test
  void rejectsOpenDrawAtCutoffInstant() {
    var rule = ruleReturning(summary(DrawStatus.OPEN, NOW));

    assertThatThrownBy(() -> rule.requireBeforeCutoff(DrawId.of(UUID.randomUUID())))
        .isInstanceOf(ProblemRestException.class)
        .hasMessageContaining("Draw cutoff time has passed");
  }

  @Test
  void rejectsDrawThatIsNotOpen() {
    var rule = ruleReturning(summary(DrawStatus.SCHEDULED, NOW.plusSeconds(60)));

    assertThatThrownBy(() -> rule.requireBeforeCutoff(DrawId.of(UUID.randomUUID())))
        .isInstanceOf(ProblemRestException.class)
        .hasMessageContaining("Draw is not open for sales");
  }

  private static DrawCutoffRule ruleReturning(DrawSummary summary) {
    return new DrawCutoffRule(new StubQueryBus(summary), Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private static DrawSummary summary(DrawStatus status, Instant cutoffAt) {
    return new DrawSummary(
        DrawId.of(UUID.randomUUID()),
        TenantId.of(UUID.randomUUID()),
        LocalDate.of(2026, 5, 6),
        status,
        Instant.parse("2026-05-06T10:00:00Z"),
        status == DrawStatus.OPEN ? NOW.minusSeconds(60) : null,
        null,
        cutoffAt,
        null,
        null,
        DrawChannelId.of(UUID.randomUUID()),
        "HT_NY_MID",
        "Haiti NY Midday",
        LocalTime.of(14, 30),
        "America/New_York",
        true,
        ResultSlotId.of(UUID.randomUUID()),
        "NY_MID",
        "NY",
        "America/New_York",
        LocalTime.of(14, 30),
        true,
        null);
  }

  private record StubQueryBus(DrawSummary summary) implements QueryBus {
    @Override
    @SuppressWarnings("unchecked")
    public <R> R send(Query<R> query) {
      return (R) summary;
    }
  }
}
