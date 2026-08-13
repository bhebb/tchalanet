package com.tchalanet.server.core.limitpolicy.internal.application.query.handler.exposure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.LimitAssignmentId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.limitpolicy.BreachOutcome;
import com.tchalanet.server.core.limitpolicy.api.RuleKey;
import com.tchalanet.server.core.limitpolicy.api.model.LimitScopeRef;
import com.tchalanet.server.core.limitpolicy.api.query.GetExposureAlertsOverviewQuery;
import com.tchalanet.server.core.limitpolicy.internal.application.port.out.assignment.LimitAssignmentReaderPort;
import com.tchalanet.server.core.limitpolicy.internal.application.port.out.exposure.ExposureAlertsReaderPort;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.LimitAssignment;
import com.tchalanet.server.core.limitpolicy.internal.domain.resolver.LimitResolver;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class GetExposureAlertsOverviewQueryHandlerTest {

  private static final JsonMapper MAPPER = JsonMapper.builder().build();
  private static final Instant NOW = Instant.parse("2026-08-12T10:00:00Z");
  private static final Clock FIXED = Clock.fixed(NOW, ZoneOffset.UTC);
  private static final TenantId TENANT =
      TenantId.of(UUID.fromString("10000000-0000-0000-0000-000000000001"));
  private static final DrawId DRAW =
      DrawId.of(UUID.fromString("20000000-0000-0000-0000-000000000001"));
  private static final DrawChannelId CHANNEL =
      DrawChannelId.of(UUID.fromString("30000000-0000-0000-0000-000000000001"));

  private final ExposureAlertsReaderPort alerts = mock(ExposureAlertsReaderPort.class);
  private final LimitAssignmentReaderPort assignments = mock(LimitAssignmentReaderPort.class);
  private final LimitResolver resolver = new LimitResolver();

  private final GetExposureAlertsOverviewQueryHandler handler =
      new GetExposureAlertsOverviewQueryHandler(alerts, assignments, resolver, FIXED);

  @Test
  void empty_reader_returns_empty_items() {
    when(alerts.topByStake(any(), any(), anyInt())).thenReturn(List.of());
    when(assignments.listActiveForTargets(any(), any())).thenReturn(List.of());

    var scope = LimitScopeRef.drawChannel(CHANNEL);
    var result = handler.handle(query(scope));

    assertThat(result.items()).isEmpty();
    assertThat(result.tenantId()).isEqualTo(TENANT);
    assertThat(result.drawId()).isEqualTo(DRAW);
  }

  @Test
  void computes_stake_ratio_when_max_stake_rule_present() {
    when(alerts.topByStake(any(), any(), anyInt()))
        .thenReturn(
            List.of(
                new ExposureAlertsReaderPort.Row(
                    BetType.MATCH_1_2D, "12", new BigDecimal("600.00"), 6L)));

    when(assignments.listActiveForTargets(any(), any()))
        .thenReturn(List.of(exposureAssignment(LimitScopeRef.drawChannel(CHANNEL))));

    var result = handler.handle(query(LimitScopeRef.drawChannel(CHANNEL)));

    assertThat(result.items()).hasSize(1);
    var item = result.items().get(0);
    assertThat(item.selectionKey()).isEqualTo("12");
    assertThat(item.stakeTotal()).isEqualByComparingTo("600.00");
    // maxStakeExposureLimit = 100000 cents → 1000.00 HTG
    assertThat(item.maxStakeExposureLimit()).isEqualByComparingTo("1000.00");
    // ratio = 600 / 1000 = 0.6000
    assertThat(item.stakeRatio()).isEqualByComparingTo("0.6000");
  }

  @Test
  void stake_ratio_is_null_when_no_exposure_rule_configured() {
    when(alerts.topByStake(any(), any(), anyInt()))
        .thenReturn(
            List.of(
                new ExposureAlertsReaderPort.Row(
                    BetType.MATCH_1_2D, "12", new BigDecimal("600.00"), 6L)));

    // no MAX_STAKE_EXPOSURE rule
    when(assignments.listActiveForTargets(any(), any())).thenReturn(List.of());

    var result = handler.handle(query(LimitScopeRef.drawChannel(CHANNEL)));

    assertThat(result.items()).hasSize(1);
    assertThat(result.items().get(0).maxStakeExposureLimit()).isNull();
    assertThat(result.items().get(0).stakeRatio()).isNull();
  }

  @Test
  void scope_key_for_draw_channel_contains_type_and_uuid() {
    when(alerts.topByStake(any(), any(), anyInt())).thenReturn(List.of());
    when(assignments.listActiveForTargets(any(), any())).thenReturn(List.of());

    var result = handler.handle(query(LimitScopeRef.drawChannel(CHANNEL)));

    assertThat(result.scopeKey()).startsWith("DRAW_CHANNEL:").contains(CHANNEL.value().toString());
  }

  @Test
  void scope_key_for_seller_terminal_contains_type_and_uuid() {
    var terminalId = com.tchalanet.server.common.types.id.SellerTerminalId.of(UUID.randomUUID());
    when(alerts.topByStake(any(), any(), anyInt())).thenReturn(List.of());
    when(assignments.listActiveForTargets(any(), any())).thenReturn(List.of());

    var result = handler.handle(query(LimitScopeRef.sellerTerminal(terminalId)));

    assertThat(result.scopeKey())
        .startsWith("SELLER_TERMINAL:")
        .contains(terminalId.value().toString());
  }

  @Test
  void multiple_rows_are_all_returned() {
    when(alerts.topByStake(any(), any(), anyInt()))
        .thenReturn(
            List.of(
                new ExposureAlertsReaderPort.Row(
                    BetType.MATCH_1_2D, "12", new BigDecimal("900"), 9L),
                new ExposureAlertsReaderPort.Row(
                    BetType.MATCH_1_2D, "34", new BigDecimal("750"), 5L)));
    when(assignments.listActiveForTargets(any(), any())).thenReturn(List.of());

    var result = handler.handle(query(LimitScopeRef.drawChannel(CHANNEL)));

    assertThat(result.items()).hasSize(2);
  }

  private GetExposureAlertsOverviewQuery query(LimitScopeRef scope) {
    return new GetExposureAlertsOverviewQuery(TENANT, DRAW, scope, 10);
  }

  private static LimitAssignment exposureAssignment(LimitScopeRef scope) {
    return LimitAssignment.createNew(
        LimitAssignmentId.of(UUID.randomUUID()),
        RuleKey.MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW,
        scope,
        true,
        BreachOutcome.BLOCK,
        // 100000 cents = 1000 HTG
        MAPPER.createObjectNode().put("valueCents", 100000L),
        null,
        null);
  }
}
