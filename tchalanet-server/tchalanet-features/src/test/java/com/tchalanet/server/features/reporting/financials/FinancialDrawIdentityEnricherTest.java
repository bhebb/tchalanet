package com.tchalanet.server.features.reporting.financials;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.analytics.api.model.TenantFinancialBreakdownView;
import com.tchalanet.server.core.draw.api.model.DrawStatus;
import com.tchalanet.server.core.draw.api.query.DrawSummary;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * The analytics projector never sees a channel code — the events carry only {@code DrawChannelId} —
 * so it stores the id in the {@code drawChannelCode} column and stamps {@code "UNKNOWN"} as the
 * game for draws resulted without a sale. Reports rendered that verbatim. These tests pin the
 * read-time repair so the raw id cannot resurface.
 */
class FinancialDrawIdentityEnricherTest {

  private static final UUID DRAW_UUID = UUID.fromString("e2def348-3ad6-47c9-995a-a75d075343ad");
  private static final UUID CHANNEL_UUID = UUID.fromString("7f000000-0000-0000-0000-000000000001");
  private static final LocalDate FROM = LocalDate.of(2026, 7, 24);
  private static final LocalDate TO = LocalDate.of(2026, 7, 30);

  @Test
  void replacesTheChannelIdWithTheChannelCode() {
    var enricher = new FinancialDrawIdentityEnricher(queryBus(drawSummary("GA-Late", "GA_LATE")));

    var enriched = enricher.enrich(viewWith(drawRow(DRAW_UUID.toString(), "UNKNOWN")), FROM, TO);

    assertThat(enriched.drawRows())
        .singleElement()
        .satisfies(
            row -> {
              assertThat(row.drawChannelCode()).isEqualTo("GA-Late");
              assertThat(row.drawChannelCode()).doesNotContain("-3ad6-");
            });
  }

  @Test
  void dropsTheUnknownGamePlaceholder() {
    var enricher = new FinancialDrawIdentityEnricher(queryBus(drawSummary("GA-Late", "GA_LATE")));

    var enriched = enricher.enrich(viewWith(drawRow(DRAW_UUID.toString(), "UNKNOWN")), FROM, TO);

    assertThat(enriched.drawRows())
        .singleElement()
        .satisfies(row -> assertThat(row.gameCode()).isNull());
  }

  @Test
  void keepsARealGameCode() {
    var enricher = new FinancialDrawIdentityEnricher(queryBus(drawSummary("GA-Late", "GA_LATE")));

    var enriched =
        enricher.enrich(viewWith(drawRow(DRAW_UUID.toString(), "HT_BORLETTE")), FROM, TO);

    assertThat(enriched.drawRows())
        .singleElement()
        .satisfies(row -> assertThat(row.gameCode()).isEqualTo("HT_BORLETTE"));
  }

  @Test
  void fallsBackToTheResultSlotKeyWhenTheChannelHasNoCode() {
    var enricher = new FinancialDrawIdentityEnricher(queryBus(drawSummary(null, "GA_LATE")));

    var enriched = enricher.enrich(viewWith(drawRow(DRAW_UUID.toString(), "UNKNOWN")), FROM, TO);

    assertThat(enriched.drawRows())
        .singleElement()
        .satisfies(row -> assertThat(row.drawChannelCode()).isEqualTo("GA_LATE"));
  }

  @Test
  void enrichesTheSellerTerminalRowsToo() {
    var enricher = new FinancialDrawIdentityEnricher(queryBus(drawSummary("GA-Late", "GA_LATE")));

    var enriched =
        enricher.enrich(viewWithSellerRow(sellerRow(DRAW_UUID.toString(), "UNKNOWN")), FROM, TO);

    assertThat(enriched.sellerTerminalDrawRows())
        .singleElement()
        .satisfies(
            row -> {
              assertThat(row.drawChannelCode()).isEqualTo("GA-Late");
              assertThat(row.gameCode()).isNull();
            });
  }

  @Test
  void leavesRowsUntouchedWhenTheDrawLookupFails() {
    var queryBus = mock(QueryBus.class);
    when(queryBus.ask(any())).thenThrow(new IllegalStateException("draw module down"));
    var enricher = new FinancialDrawIdentityEnricher(queryBus);

    var enriched = enricher.enrich(viewWith(drawRow(DRAW_UUID.toString(), "UNKNOWN")), FROM, TO);

    // A report that renders a raw id still beats a report that 500s.
    assertThat(enriched.drawRows())
        .singleElement()
        .satisfies(row -> assertThat(row.drawChannelCode()).isEqualTo(DRAW_UUID.toString()));
  }

  // ── fixtures ───────────────────────────────────────────────────────────────

  private static QueryBus queryBus(DrawSummary summary) {
    var bus = mock(QueryBus.class);
    when(bus.ask(any()))
        .thenReturn(TchPage.of(List.of(summary), 0, 1000, 1L, 1, true, false, false));
    return bus;
  }

  private static DrawSummary drawSummary(String channelCode, String slotKey) {
    return new DrawSummary(
        DrawId.of(DRAW_UUID),
        TenantId.of(UUID.randomUUID()),
        TO,
        DrawStatus.RESULTED,
        Instant.parse("2026-07-30T20:00:00Z"),
        null,
        null,
        null,
        null,
        null,
        DrawChannelId.of(CHANNEL_UUID),
        channelCode,
        "Georgia Late",
        null,
        "America/New_York",
        true,
        null,
        slotKey,
        "GA",
        "America/New_York",
        null,
        true,
        null);
  }

  private static TenantFinancialBreakdownView.DrawFinancialRow drawRow(
      String storedChannelCode, String storedGameCode) {

    return new TenantFinancialBreakdownView.DrawFinancialRow(
        DRAW_UUID,
        TO,
        Instant.parse("2026-07-30T20:00:00Z"),
        storedGameCode,
        storedChannelCode,
        0L,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        0L,
        0L,
        BigDecimal.ZERO,
        BigDecimal.ZERO);
  }

  private static TenantFinancialBreakdownView.SellerTerminalDrawFinancialRow sellerRow(
      String storedChannelCode, String storedGameCode) {

    return new TenantFinancialBreakdownView.SellerTerminalDrawFinancialRow(
        UUID.randomUUID(),
        DRAW_UUID,
        TO,
        Instant.parse("2026-07-30T20:00:00Z"),
        storedGameCode,
        storedChannelCode,
        0L,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        0L,
        0L,
        BigDecimal.ZERO,
        BigDecimal.ZERO);
  }

  private static TenantFinancialBreakdownView viewWith(
      TenantFinancialBreakdownView.DrawFinancialRow row) {
    return new TenantFinancialBreakdownView(
        FROM, TO, summary(), List.of(), List.of(row), List.of(), List.of());
  }

  private static TenantFinancialBreakdownView viewWithSellerRow(
      TenantFinancialBreakdownView.SellerTerminalDrawFinancialRow row) {
    return new TenantFinancialBreakdownView(
        FROM, TO, summary(), List.of(), List.of(), List.of(row), List.of());
  }

  private static TenantFinancialBreakdownView.FinancialSummary summary() {
    return new TenantFinancialBreakdownView.FinancialSummary(
        0L,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        0L,
        0L,
        BigDecimal.ZERO,
        BigDecimal.ZERO);
  }
}
