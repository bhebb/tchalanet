package com.tchalanet.server.core.analytics.internal.application.service;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.core.analytics.api.command.ReconcileAnalyticsCommand;
import com.tchalanet.server.core.analytics.api.model.AnalyticsMetricSnapshot;
import com.tchalanet.server.core.analytics.api.model.AnalyticsReconciliationMismatch;
import com.tchalanet.server.core.analytics.api.model.AnalyticsReconciliationMode;
import com.tchalanet.server.core.analytics.api.model.AnalyticsReconciliationResult;
import com.tchalanet.server.core.analytics.api.model.AnalyticsReconciliationStatus;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDailyEntity;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDailyRepository;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDrawEntity;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDrawRepository;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsSelectionEntity;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsSelectionRepository;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsSellerTerminalDrawEntity;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsSellerTerminalDrawRepository;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsTenantProjectionLock;
import com.tchalanet.server.core.sales.api.model.analytics.SalesAnalyticsTicketChargeSnapshot;
import com.tchalanet.server.core.sales.api.model.analytics.SalesAnalyticsTicketLineSnapshot;
import com.tchalanet.server.core.sales.api.model.analytics.SalesAnalyticsTicketSnapshot;
import com.tchalanet.server.core.sales.api.query.GetSalesAnalyticsTicketSnapshotsQuery;
import com.tchalanet.server.platform.tenant.api.TenantZoneApi;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rebuilds analytics projections from immutable sales snapshots, never by replaying domain events.
 *
 * <p>The source aggregation intentionally mirrors the established projector semantics. In
 * particular, cancellation changes daily ticket counts on its lifecycle day while a draw row keeps
 * the financial story of the original sale.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsReconciliationService {

  private final QueryBus queryBus;
  private final TenantZoneApi tenantZoneApi;
  private final AnalyticsDailyRepository dailyRepository;
  private final AnalyticsDrawRepository drawRepository;
  private final AnalyticsSellerTerminalDrawRepository sellerTerminalDrawRepository;
  private final AnalyticsSelectionRepository selectionRepository;
  private final AnalyticsTenantProjectionLock projectionLock;
  private final Clock clock;

  @Transactional(readOnly = true)
  public AnalyticsReconciliationResult validate(ReconcileAnalyticsCommand command) {
    var expected = expected(command);
    return result(command, expected, observed(command), AnalyticsReconciliationMode.VALIDATE);
  }

  @Transactional
  public AnalyticsReconciliationResult rebuildAndValidate(ReconcileAnalyticsCommand command) {
    projectionLock.acquire(command.tenantId());
    var expected = expected(command);
    replace(command, expected);
    var result =
        result(
            command, expected, observed(command), AnalyticsReconciliationMode.REBUILD_AND_VALIDATE);
    if (result.status() != AnalyticsReconciliationStatus.SUCCESS) {
      throw new IllegalStateException("analytics.reconciliation.rebuild_not_exact");
    }
    return result;
  }

  private AnalyticsReconciliationResult result(
      ReconcileAnalyticsCommand command,
      ProjectionSet expected,
      ProjectionSet observed,
      AnalyticsReconciliationMode mode) {
    var mismatches = compare(expected, observed);
    return new AnalyticsReconciliationResult(
        UUID.randomUUID(),
        command.tenantId(),
        command.from(),
        command.to(),
        mode,
        mismatches.isEmpty()
            ? AnalyticsReconciliationStatus.SUCCESS
            : AnalyticsReconciliationStatus.MISMATCH,
        mismatches,
        Instant.now(clock));
  }

  private ProjectionSet expected(ReconcileAnalyticsCommand command) {
    ZoneId zone = tenantZone(command);
    Instant from = command.from().atStartOfDay(zone).toInstant();
    Instant to = command.to().plusDays(1).atStartOfDay(zone).toInstant();
    var snapshots =
        queryBus.ask(
            new GetSalesAnalyticsTicketSnapshotsQuery(
                command.tenantId(), from, to, command.from(), command.to()));
    return ProjectionSet.fromSnapshots(snapshots, command.from(), command.to(), zone);
  }

  private ProjectionSet observed(ReconcileAnalyticsCommand command) {
    var set = new ProjectionSet();
    for (var row :
        dailyRepository.findTenantRows(command.tenantId().value(), command.from(), command.to())) {
      set.daily.put(new DailyKey("TENANT", null, row.getRefDate()), Totals.from(row));
    }
    for (var row :
        dailyRepository.findSellerTerminalRows(
            command.tenantId().value(), command.from(), command.to())) {
      set.daily.put(
          new DailyKey("SELLER_TERMINAL", row.getDimensionId(), row.getRefDate()),
          Totals.from(row));
    }
    for (var row :
        drawRepository.findByTenantIdAndRefDateBetweenOrderByRefDate(
            command.tenantId().value(), command.from(), command.to())) {
      set.draws.put(new DrawKey(row.getDrawId()), Totals.from(row));
    }
    for (var row :
        sellerTerminalDrawRepository.findByTenantIdAndRefDateBetweenOrderByRefDateDescUpdatedAtDesc(
            command.tenantId().value(), command.from(), command.to())) {
      set.sellerTerminalDraws.put(
          new SellerTerminalDrawKey(row.getSellerTerminalId(), row.getDrawId()), Totals.from(row));
    }
    for (var row :
        selectionRepository.findByTenantIdAndRefDateBetweenOrderByRefDate(
            command.tenantId().value(), command.from(), command.to())) {
      set.selections.put(SelectionKey.from(row), Totals.from(row));
    }
    return set;
  }

  private void replace(ReconcileAnalyticsCommand command, ProjectionSet expected) {
    UUID tenantId = command.tenantId().value();
    dailyRepository.deleteTenantProjectionRows(tenantId, command.from(), command.to());
    drawRepository.deleteTenantRows(tenantId, command.from(), command.to());
    sellerTerminalDrawRepository.deleteTenantRows(tenantId, command.from(), command.to());
    selectionRepository.deleteTenantRows(tenantId, command.from(), command.to());

    Instant now = Instant.now(clock);
    dailyRepository.saveAll(expected.dailyEntities(tenantId, now));
    drawRepository.saveAll(expected.drawEntities(tenantId, now));
    sellerTerminalDrawRepository.saveAll(expected.sellerTerminalDrawEntities(tenantId, now));
    selectionRepository.saveAll(expected.selectionEntities(tenantId, now));
    dailyRepository.flush();
    drawRepository.flush();
    sellerTerminalDrawRepository.flush();
    selectionRepository.flush();
  }

  private List<AnalyticsReconciliationMismatch> compare(
      ProjectionSet expected, ProjectionSet observed) {
    var mismatches = new ArrayList<AnalyticsReconciliationMismatch>();
    compareDaily(expected.daily, observed.daily, mismatches);
    compareDraws(expected.draws, observed.draws, mismatches);
    compareSellerTerminalDraws(
        expected.sellerTerminalDraws, observed.sellerTerminalDraws, mismatches);
    compareSelections(expected.selections, observed.selections, mismatches);
    return List.copyOf(mismatches);
  }

  private static void compareDaily(
      Map<DailyKey, Totals> expected,
      Map<DailyKey, Totals> observed,
      List<AnalyticsReconciliationMismatch> mismatches) {
    for (var key : union(expected, observed)) {
      var left = expected.getOrDefault(key, Totals.ZERO);
      var right = observed.getOrDefault(key, Totals.ZERO);
      if (!left.equals(right)) {
        mismatches.add(
            new AnalyticsReconciliationMismatch(
                "DAILY_" + key.dimensionType(),
                key.refDate(),
                null,
                key.sellerTerminalId(),
                left.snapshot(),
                right.snapshot()));
      }
    }
  }

  private static void compareDraws(
      Map<DrawKey, Totals> expected,
      Map<DrawKey, Totals> observed,
      List<AnalyticsReconciliationMismatch> mismatches) {
    for (var key : union(expected, observed)) {
      var left = expected.getOrDefault(key, Totals.ZERO);
      var right = observed.getOrDefault(key, Totals.ZERO);
      if (!left.equals(right)) {
        mismatches.add(
            new AnalyticsReconciliationMismatch(
                "DRAW", null, key.drawId(), null, left.snapshot(), right.snapshot()));
      }
    }
  }

  private static void compareSellerTerminalDraws(
      Map<SellerTerminalDrawKey, Totals> expected,
      Map<SellerTerminalDrawKey, Totals> observed,
      List<AnalyticsReconciliationMismatch> mismatches) {
    for (var key : union(expected, observed)) {
      var left = expected.getOrDefault(key, Totals.ZERO);
      var right = observed.getOrDefault(key, Totals.ZERO);
      if (!left.equals(right)) {
        mismatches.add(
            new AnalyticsReconciliationMismatch(
                "SELLER_TERMINAL_DRAW",
                null,
                key.drawId(),
                key.sellerTerminalId(),
                left.snapshot(),
                right.snapshot()));
      }
    }
  }

  private static void compareSelections(
      Map<SelectionKey, Totals> expected,
      Map<SelectionKey, Totals> observed,
      List<AnalyticsReconciliationMismatch> mismatches) {
    for (var key : union(expected, observed)) {
      var left = expected.getOrDefault(key, Totals.ZERO);
      var right = observed.getOrDefault(key, Totals.ZERO);
      if (!left.equals(right)) {
        mismatches.add(
            new AnalyticsReconciliationMismatch(
                "SELECTION:" + key.selectionKey(),
                key.refDate(),
                null,
                null,
                left.snapshot(),
                right.snapshot()));
      }
    }
  }

  private ZoneId tenantZone(ReconcileAnalyticsCommand command) {
    var zone = tenantZoneApi.resolveTenantZone(command.tenantId());
    return zone != null ? zone : ZoneOffset.UTC;
  }

  private static <T> Set<T> union(Map<T, ?> left, Map<T, ?> right) {
    var keys = new HashSet<T>(left.keySet());
    keys.addAll(right.keySet());
    return keys;
  }

  private record DailyKey(String dimensionType, UUID sellerTerminalId, LocalDate refDate) {}

  private record DrawKey(UUID drawId) {}

  private record SellerTerminalDrawKey(UUID sellerTerminalId, UUID drawId) {}

  private record SelectionKey(
      LocalDate refDate,
      UUID drawChannelId,
      String gameCode,
      String betType,
      Short betOption,
      String selectionKey) {
    static SelectionKey from(AnalyticsSelectionEntity row) {
      return new SelectionKey(
          row.getRefDate(),
          row.getDrawChannelId(),
          row.getGameCode(),
          row.getBetType(),
          row.getBetOption(),
          row.getSelectionKey());
    }
  }

  private static final class ProjectionSet {
    private final Map<DailyKey, Totals> daily = new HashMap<>();
    private final Map<DrawKey, Totals> draws = new HashMap<>();
    private final Map<SellerTerminalDrawKey, Totals> sellerTerminalDraws = new HashMap<>();
    private final Map<SelectionKey, Totals> selections = new HashMap<>();
    private final Map<DrawKey, DrawMetadata> drawMetadata = new HashMap<>();
    private final Map<SellerTerminalDrawKey, DrawMetadata> sellerTerminalDrawMetadata =
        new HashMap<>();

    static ProjectionSet fromSnapshots(
        Collection<SalesAnalyticsTicketSnapshot> snapshots,
        LocalDate from,
        LocalDate to,
        ZoneId zone) {
      var result = new ProjectionSet();
      for (var ticket : snapshots) {
        boolean officialSale = ticket.soldAt() != null;
        LocalDate soldDate = date(ticket.soldAt(), zone);
        if (officialSale && inScope(soldDate, from, to)) {
          result
              .daily
              .computeIfAbsent(new DailyKey("TENANT", null, soldDate), ignored -> new Totals())
              .addSale(ticket);
          if (ticket.sellerTerminalId() != null) {
            result
                .daily
                .computeIfAbsent(
                    new DailyKey("SELLER_TERMINAL", ticket.sellerTerminalId(), soldDate),
                    ignored -> new Totals())
                .addSale(ticket);
          }
          result.addSelections(ticket, soldDate);
        }

        LocalDate cancellationDate = date(ticket.cancelledAt(), zone);
        if (inScope(cancellationDate, from, to)) {
          result
              .daily
              .computeIfAbsent(
                  new DailyKey("TENANT", null, cancellationDate), ignored -> new Totals())
              .addCancellation();
        }

        LocalDate paymentDate =
            date(ticket.paidAt() != null ? ticket.paidAt() : ticket.settledAt(), zone);
        if (inScope(paymentDate, from, to)) {
          result
              .daily
              .computeIfAbsent(new DailyKey("TENANT", null, paymentDate), ignored -> new Totals())
              .addSettlement(ticket.winningAmount(), ticket.winningAmount());
        }
        LocalDate adjustmentDate = date(ticket.paidAmountAdjustedAt(), zone);
        if (inScope(adjustmentDate, from, to)) {
          result
              .daily
              .computeIfAbsent(
                  new DailyKey("TENANT", null, adjustmentDate), ignored -> new Totals())
              .addPaid(delta(ticket.paidAmount(), ticket.winningAmount()));
        }

        LocalDate drawDate = ticket.drawDate();
        if (officialSale && inScope(drawDate, from, to) && ticket.drawId() != null) {
          var drawKey = new DrawKey(ticket.drawId());
          result.draws.computeIfAbsent(drawKey, ignored -> new Totals()).addFinalDraw(ticket);
          result.drawMetadata.putIfAbsent(drawKey, DrawMetadata.from(ticket, drawDate));
          if (ticket.sellerTerminalId() != null) {
            var terminalDrawKey =
                new SellerTerminalDrawKey(ticket.sellerTerminalId(), ticket.drawId());
            result
                .sellerTerminalDraws
                .computeIfAbsent(terminalDrawKey, ignored -> new Totals())
                .addFinalDraw(ticket);
            result.sellerTerminalDrawMetadata.putIfAbsent(
                terminalDrawKey, DrawMetadata.from(ticket, drawDate));
          }
        }
      }
      return result;
    }

    private void addSelections(SalesAnalyticsTicketSnapshot ticket, LocalDate refDate) {
      for (var line : ticket.lines()) {
        var key =
            new SelectionKey(
                refDate,
                ticket.drawChannelId(),
                line.gameCode(),
                line.betType(),
                line.betOption(),
                line.selectionKey());
        selections.computeIfAbsent(key, ignored -> new Totals()).addSelection(line);
      }
    }

    List<AnalyticsDailyEntity> dailyEntities(UUID tenantId, Instant now) {
      return daily.entrySet().stream()
          .map(entry -> entry.getValue().dailyEntity(entry.getKey(), tenantId, now))
          .toList();
    }

    List<AnalyticsDrawEntity> drawEntities(UUID tenantId, Instant now) {
      return draws.entrySet().stream()
          .map(
              entry ->
                  entry
                      .getValue()
                      .drawEntity(entry.getKey(), drawMetadata.get(entry.getKey()), tenantId, now))
          .toList();
    }

    List<AnalyticsSellerTerminalDrawEntity> sellerTerminalDrawEntities(UUID tenantId, Instant now) {
      return sellerTerminalDraws.entrySet().stream()
          .map(
              entry ->
                  entry
                      .getValue()
                      .sellerTerminalDrawEntity(
                          entry.getKey(),
                          sellerTerminalDrawMetadata.get(entry.getKey()),
                          tenantId,
                          now))
          .toList();
    }

    List<AnalyticsSelectionEntity> selectionEntities(UUID tenantId, Instant now) {
      return selections.entrySet().stream()
          .map(entry -> entry.getValue().selectionEntity(entry.getKey(), tenantId, now))
          .toList();
    }
  }

  private record DrawMetadata(
      LocalDate refDate, Instant scheduledAt, String gameCode, String drawChannelCode) {
    static DrawMetadata from(SalesAnalyticsTicketSnapshot ticket, LocalDate refDate) {
      var gameCodes =
          ticket.lines().stream()
              .map(SalesAnalyticsTicketLineSnapshot::gameCode)
              .filter(value -> value != null && !value.isBlank())
              .distinct()
              .toList();
      return new DrawMetadata(
          refDate,
          ticket.drawScheduledAt(),
          gameCodes.size() == 1 ? gameCodes.getFirst() : "MIXED",
          ticket.drawChannelId() == null ? null : ticket.drawChannelId().toString());
    }
  }

  private static final class Totals {
    static final Totals ZERO = new Totals();
    private long ticketsSold;
    private long ticketsCancelled;
    private long grossSales;
    private long stake;
    private long winnings;
    private long paid;
    private long commission;
    private long buyerCharge;
    private long sellerCharge;
    private long tenantCharge;
    private long waivedCharge;
    private long promotionLines;
    private long promotionPricedLines;

    static Totals from(AnalyticsDailyEntity row) {
      var result = new Totals();
      result.ticketsSold = row.getTicketsSoldCount();
      result.ticketsCancelled = row.getTicketsCancelledCount();
      result.grossSales = row.getGrossSalesCents();
      result.stake = row.getStakeTotalCents();
      result.winnings = row.getWinningsCalculatedCents();
      result.paid = row.getPayoutsPaidCents();
      result.commission = row.getSellerCommissionCents();
      result.buyerCharge = row.getBuyerChargeCents();
      result.sellerCharge = row.getSellerChargeCents();
      result.tenantCharge = row.getTenantChargeCents();
      result.waivedCharge = row.getWaivedChargeCents();
      result.promotionLines = row.getPromotionLineCount();
      result.promotionPricedLines = row.getPromotionPricedLineCount();
      return result;
    }

    static Totals from(AnalyticsDrawEntity row) {
      var result = new Totals();
      result.ticketsSold = row.getTicketsSoldCount();
      result.ticketsCancelled = row.getTicketsCancelledCount();
      result.grossSales = row.getGrossSalesCents();
      result.stake = row.getStakeTotalCents();
      result.winnings = row.getWinningsCalculatedCents();
      result.paid = row.getPayoutsPaidCents();
      result.commission = row.getSellerCommissionCents();
      result.buyerCharge = row.getBuyerChargeCents();
      result.sellerCharge = row.getSellerChargeCents();
      result.tenantCharge = row.getTenantChargeCents();
      result.waivedCharge = row.getWaivedChargeCents();
      result.promotionLines = row.getPromotionLineCount();
      result.promotionPricedLines = row.getPromotionPricedLineCount();
      return result;
    }

    static Totals from(AnalyticsSellerTerminalDrawEntity row) {
      var result = new Totals();
      result.ticketsSold = row.getTicketsSoldCount();
      result.grossSales = row.getGrossSalesCents();
      result.stake = row.getStakeTotalCents();
      result.winnings = row.getWinningsCalculatedCents();
      result.paid = row.getPayoutsPaidCents();
      result.commission = row.getSellerCommissionCents();
      result.buyerCharge = row.getBuyerChargeCents();
      result.sellerCharge = row.getSellerChargeCents();
      result.tenantCharge = row.getTenantChargeCents();
      result.waivedCharge = row.getWaivedChargeCents();
      result.promotionLines = row.getPromotionLineCount();
      result.promotionPricedLines = row.getPromotionPricedLineCount();
      return result;
    }

    static Totals from(AnalyticsSelectionEntity row) {
      var result = new Totals();
      result.ticketsSold = row.getTicketsCount();
      result.stake = row.getStakeSumCents();
      result.winnings = row.getWinningsCalculatedCents();
      return result;
    }

    void addSale(SalesAnalyticsTicketSnapshot ticket) {
      ticketsSold++;
      long amount = cents(ticket.stakeAmount());
      grossSales += amount;
      stake += amount;
      commission += cents(ticket.sellerCommissionAmount());
      addCharges(ticket.charges());
      for (var line : ticket.lines()) {
        if ("PROMOTION".equals(line.origin())) promotionLines++;
        if ("PROMOTION".equals(line.pricingSource())) promotionPricedLines++;
      }
    }

    void addCancellation() {
      ticketsSold--;
      ticketsCancelled++;
    }

    void addSettlement(BigDecimal winningAmount, BigDecimal paidAmount) {
      winnings += cents(winningAmount);
      paid += cents(paidAmount);
    }

    void addPaid(BigDecimal amount) {
      paid += cents(amount);
    }

    void addFinalDraw(SalesAnalyticsTicketSnapshot ticket) {
      addSale(ticket);
      winnings += cents(ticket.winningAmount());
      paid += cents(ticket.paidAmount());
    }

    void addSelection(SalesAnalyticsTicketLineSnapshot line) {
      ticketsSold++;
      stake += cents(line.stakeAmount());
    }

    private void addCharges(List<SalesAnalyticsTicketChargeSnapshot> charges) {
      for (var charge : charges) {
        long amount = cents(charge.amount());
        if (charge.waived()) {
          waivedCharge += amount;
          continue;
        }
        switch (charge.paidBy()) {
          case "BUYER" -> buyerCharge += amount;
          case "SELLER" -> sellerCharge += amount;
          case "TENANT" -> tenantCharge += amount;
          default -> {}
        }
      }
    }

    AnalyticsMetricSnapshot snapshot() {
      return new AnalyticsMetricSnapshot(
          ticketsSold,
          ticketsCancelled,
          grossSales,
          stake,
          winnings,
          paid,
          commission,
          buyerCharge,
          sellerCharge,
          tenantCharge,
          waivedCharge,
          promotionLines,
          promotionPricedLines);
    }

    AnalyticsDailyEntity dailyEntity(DailyKey key, UUID tenantId, Instant now) {
      return AnalyticsDailyEntity.builder()
          .dimensionType(key.dimensionType())
          .dimensionId(key.sellerTerminalId())
          .tenantId(tenantId)
          .refDate(key.refDate())
          .ticketsSoldCount(ticketsSold)
          .ticketsCancelledCount(ticketsCancelled)
          .grossSalesCents(grossSales)
          .stakeTotalCents(stake)
          .winningsCalculatedCents(winnings)
          .payoutsPaidCents(paid)
          .sellerCommissionCents(commission)
          .buyerChargeCents(buyerCharge)
          .sellerChargeCents(sellerCharge)
          .tenantChargeCents(tenantCharge)
          .waivedChargeCents(waivedCharge)
          .promotionLineCount(promotionLines)
          .promotionPricedLineCount(promotionPricedLines)
          .netRevenueEstimatedCents(grossSales - winnings - commission - tenantCharge)
          .netRevenuePaidBasisCents(grossSales - paid - commission - tenantCharge)
          .sessionsOpenedCount(0)
          .sessionsClosedCount(0)
          .createdAt(now)
          .updatedAt(now)
          .build();
    }

    AnalyticsDrawEntity drawEntity(DrawKey key, DrawMetadata metadata, UUID tenantId, Instant now) {
      return AnalyticsDrawEntity.builder()
          .drawId(key.drawId())
          .tenantId(tenantId)
          .gameCode(metadata.gameCode())
          .drawChannelCode(metadata.drawChannelCode())
          .scheduledAt(metadata.scheduledAt())
          .refDate(metadata.refDate())
          .ticketsSoldCount(ticketsSold)
          .ticketsCancelledCount(ticketsCancelled)
          .grossSalesCents(grossSales)
          .stakeTotalCents(stake)
          .winningsCalculatedCents(winnings)
          .payoutsPaidCents(paid)
          .sellerCommissionCents(commission)
          .buyerChargeCents(buyerCharge)
          .sellerChargeCents(sellerCharge)
          .tenantChargeCents(tenantCharge)
          .waivedChargeCents(waivedCharge)
          .promotionLineCount(promotionLines)
          .promotionPricedLineCount(promotionPricedLines)
          .netRevenueEstimatedCents(grossSales - winnings - commission - tenantCharge)
          .netRevenuePaidBasisCents(grossSales - paid - commission - tenantCharge)
          .createdAt(now)
          .updatedAt(now)
          .build();
    }

    AnalyticsSellerTerminalDrawEntity sellerTerminalDrawEntity(
        SellerTerminalDrawKey key, DrawMetadata metadata, UUID tenantId, Instant now) {
      return AnalyticsSellerTerminalDrawEntity.builder()
          .tenantId(tenantId)
          .sellerTerminalId(key.sellerTerminalId())
          .drawId(key.drawId())
          .refDate(metadata.refDate())
          .scheduledAt(metadata.scheduledAt())
          .gameCode(metadata.gameCode())
          .drawChannelCode(metadata.drawChannelCode())
          .ticketsSoldCount(ticketsSold)
          .grossSalesCents(grossSales)
          .stakeTotalCents(stake)
          .winningsCalculatedCents(winnings)
          .payoutsPaidCents(paid)
          .sellerCommissionCents(commission)
          .buyerChargeCents(buyerCharge)
          .sellerChargeCents(sellerCharge)
          .tenantChargeCents(tenantCharge)
          .waivedChargeCents(waivedCharge)
          .promotionLineCount(promotionLines)
          .promotionPricedLineCount(promotionPricedLines)
          .netRevenueEstimatedCents(grossSales - winnings - commission - tenantCharge)
          .netRevenuePaidBasisCents(grossSales - paid - commission - tenantCharge)
          .createdAt(now)
          .updatedAt(now)
          .build();
    }

    AnalyticsSelectionEntity selectionEntity(SelectionKey key, UUID tenantId, Instant now) {
      return AnalyticsSelectionEntity.builder()
          .tenantId(tenantId)
          .refDate(key.refDate())
          .drawChannelId(key.drawChannelId())
          .gameCode(key.gameCode())
          .betType(key.betType())
          .betOption(key.betOption())
          .selectionKey(key.selectionKey())
          .ticketsCount(ticketsSold)
          .stakeSumCents(stake)
          .winningsCalculatedCents(winnings)
          .createdAt(now)
          .updatedAt(now)
          .build();
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof Totals totals && snapshot().equals(totals.snapshot());
    }

    @Override
    public int hashCode() {
      return snapshot().hashCode();
    }
  }

  private static boolean inScope(LocalDate date, LocalDate from, LocalDate to) {
    return date != null && !date.isBefore(from) && !date.isAfter(to);
  }

  private static LocalDate date(Instant instant, ZoneId zone) {
    return instant == null ? null : LocalDate.ofInstant(instant, zone);
  }

  private static BigDecimal delta(BigDecimal paid, BigDecimal winning) {
    return value(paid).subtract(value(winning));
  }

  private static long cents(BigDecimal value) {
    return value == null ? 0L : value.movePointRight(2).longValue();
  }

  private static BigDecimal value(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }
}
