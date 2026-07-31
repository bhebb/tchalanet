package com.tchalanet.server.features.reporting.financials;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.analytics.api.model.TenantFinancialBreakdownView;
import com.tchalanet.server.core.draw.api.query.DrawSearchCriteria;
import com.tchalanet.server.core.draw.api.query.DrawSummary;
import com.tchalanet.server.core.draw.api.query.ListDrawsQuery;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Replaces the draw identity carried by the analytics rows with the real one.
 *
 * <p>The analytics projector only ever sees {@code DrawChannelId} — the sale and result events
 * carry no channel code — so it stores the channel <em>id</em> in a column named {@code
 * drawChannelCode}, and stamps {@code gameCode} as {@code "UNKNOWN"} for draws that were resulted
 * without any sale. Reports therefore rendered rows like {@code
 * "e2def348-3ad6-47c9-995a-a75d075343ad · UNKNOWN"}.
 *
 * <p>Resolving here rather than at projection time keeps the write path free of a cross-module
 * lookup and, more importantly, fixes rows that were already written — there is nothing to
 * backfill. The draw module's {@code DrawSummary} already exposes the code, the label and the
 * result slot, and the breakdown is bounded by the same date window, so one paged read covers it.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FinancialDrawIdentityEnricher {

  /**
   * Draws per day stay in the low tens (a handful of channels times their slots), and the window is
   * a reporting range rather than an archive scan, so a single page is enough. If a tenant ever
   * exceeds this the rows beyond it simply keep the id they already had.
   */
  private static final int MAX_DRAWS = 1000;

  private final QueryBus queryBus;

  public TenantFinancialBreakdownView enrich(
      TenantFinancialBreakdownView view, LocalDate from, LocalDate to) {

    Map<UUID, DrawSummary> draws = drawsByIdInRange(from, to);
    if (draws.isEmpty()) {
      return view;
    }

    return new TenantFinancialBreakdownView(
        view.from(),
        view.to(),
        view.summary(),
        view.dailyRows(),
        view.drawRows().stream().map(row -> enrichDrawRow(row, draws.get(row.drawId()))).toList(),
        view.sellerTerminalDrawRows().stream()
            .map(row -> enrichSellerTerminalDrawRow(row, draws.get(row.drawId())))
            .toList(),
        view.sellerTerminalDailyRows());
  }

  private Map<UUID, DrawSummary> drawsByIdInRange(LocalDate from, LocalDate to) {
    try {
      TchPage<DrawSummary> page =
          queryBus.ask(
              new ListDrawsQuery(
                  DrawSearchCriteria.of(null, null, from, to), PageRequest.of(0, MAX_DRAWS)));

      Map<UUID, DrawSummary> byId = new HashMap<>();
      for (DrawSummary draw : page.items()) {
        byId.put(draw.drawId().value(), draw);
      }
      return byId;
    } catch (RuntimeException ex) {
      // Identity is a presentation nicety; never fail a financial report over it.
      log.warn("financials.draw_identity_unavailable from={} to={}", from, to, ex);
      return Map.of();
    }
  }

  private TenantFinancialBreakdownView.DrawFinancialRow enrichDrawRow(
      TenantFinancialBreakdownView.DrawFinancialRow row, DrawSummary draw) {

    if (draw == null) {
      return row;
    }

    return new TenantFinancialBreakdownView.DrawFinancialRow(
        row.drawId(),
        row.refDate(),
        row.scheduledAt(),
        resolveGameCode(row.gameCode()),
        resolveChannelCode(row.drawChannelCode(), draw),
        row.ticketsSold(),
        row.grossSales(),
        row.winningsCalculated(),
        row.payoutsPaid(),
        row.sellerCommission(),
        row.buyerCharges(),
        row.sellerCharges(),
        row.tenantCharges(),
        row.waivedCharges(),
        row.promotionLines(),
        row.promotionPricedLines(),
        row.netRevenueEstimated(),
        row.netRevenuePaidBasis());
  }

  private TenantFinancialBreakdownView.SellerTerminalDrawFinancialRow enrichSellerTerminalDrawRow(
      TenantFinancialBreakdownView.SellerTerminalDrawFinancialRow row, DrawSummary draw) {

    if (draw == null) {
      return row;
    }

    return new TenantFinancialBreakdownView.SellerTerminalDrawFinancialRow(
        row.sellerTerminalId(),
        row.drawId(),
        row.refDate(),
        row.scheduledAt(),
        resolveGameCode(row.gameCode()),
        resolveChannelCode(row.drawChannelCode(), draw),
        row.ticketsSold(),
        row.grossSales(),
        row.winningsCalculated(),
        row.payoutsPaid(),
        row.sellerCommission(),
        row.buyerCharges(),
        row.sellerCharges(),
        row.tenantCharges(),
        row.waivedCharges(),
        row.promotionLines(),
        row.promotionPricedLines(),
        row.netRevenueEstimated(),
        row.netRevenuePaidBasis());
  }

  /** Prefers the channel code; falls back to the slot key, then to whatever was stored. */
  private static String resolveChannelCode(String stored, DrawSummary draw) {
    if (isPresent(draw.drawChannelCode())) {
      return draw.drawChannelCode();
    }
    if (isPresent(draw.resultSlotKey())) {
      return draw.resultSlotKey();
    }
    return stored;
  }

  /**
   * A draw carries no game of its own — games live on the ticket lines — so a draw that was
   * resulted without a sale has none to report. Dropping the literal "UNKNOWN" lets the client fall
   * back to the channel alone instead of printing a word that means nothing to a reader.
   */
  private static String resolveGameCode(String stored) {
    if (isPresent(stored) && !"UNKNOWN".equals(stored)) {
      return stored;
    }
    return null;
  }

  private static boolean isPresent(String value) {
    return value != null && !value.isBlank();
  }
}
