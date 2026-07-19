package com.tchalanet.server.features.ops.sales;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.constant.CommonConstants;
import com.tchalanet.server.common.context.TchActorType;
import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.context.TchContextScope;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.core.draw.api.query.DrawSummary;
import com.tchalanet.server.core.draw.api.query.GetDrawByIdQuery;
import com.tchalanet.server.core.sales.api.command.preparation.ConfirmPreparedSaleCommand;
import com.tchalanet.server.core.sales.api.command.preparation.PrepareSaleCommand;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketOutcome;
import com.tchalanet.server.core.sales.api.model.communication.SaleCommunicationOptions;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalView;
import com.tchalanet.server.core.sellerterminal.api.query.GetSellerTerminalQuery;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Currency;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class OpsSalesSimulationService {

  private static final int DEFAULT_MAX_TICKETS = 5_000;

  private final CommandBus commandBus;
  private final QueryBus queryBus;
  private final OpsSalesSimulationPlanner planner = new OpsSalesSimulationPlanner();

  OpsSalesSimulationResponse simulate(OpsSalesSimulationRequest request) {
    var normalized = normalize(request);
    int requestedTickets = planner.requestedTickets(normalized);
    if (requestedTickets <= 0) {
      throw ProblemRest.badRequest("ops.sales_simulation.empty_mix");
    }
    int maxTickets =
        normalized.maxTickets() == null ? DEFAULT_MAX_TICKETS : normalized.maxTickets();
    if (maxTickets <= 0 || requestedTickets > maxTickets) {
      throw ProblemRest.badRequest(
          "ops.sales_simulation.too_many_tickets: requested="
              + requestedTickets
              + ", max="
              + maxTickets);
    }
    if (!normalized.dryRun() && (normalized.reason() == null || normalized.reason().isBlank())) {
      throw ProblemRest.badRequest("ops.sales_simulation.reason_required");
    }

    var simulationId = UUID.randomUUID();
    long seed =
        normalized.seed() == null ? simulationId.getMostSignificantBits() : normalized.seed();
    var planned = planner.plan(normalized, normalized.stakeAmount(), seed);
    var tenantId = TenantId.of(normalized.tenantId());
    var currency = CurrencyCode.of(normalized.currency());
    var tenantContext =
        tenantContext(
            normalized.tenantId(), normalized.currency(), "ops-sales-sim-" + simulationId);
    var draws = loadDraws(tenantContext, tenantId, normalized.drawIds());
    var sellers = loadSellers(tenantContext, tenantId, normalized.sellerTerminalIds());
    var stats = new Stats();
    stats.addPlanned(planned);

    if (!normalized.dryRun()) {
      for (var ticket : planned) {
        executeOne(normalized, simulationId, tenantId, currency, draws, ticket, stats);
      }
    }

    return stats.toResponse(
        simulationId, normalized, seed, requestedTickets, planned.size(), draws, sellers);
  }

  private void executeOne(
      OpsSalesSimulationRequest request,
      UUID simulationId,
      TenantId tenantId,
      CurrencyCode currency,
      Map<UUID, DrawSummary> draws,
      OpsSalesSimulationPlanner.PlannedTicket ticket,
      Stats stats) {
    var draw = draws.get(ticket.drawId());
    var sellerContext =
        sellerContext(
            tenantId.value(),
            ticket.sellerTerminalId(),
            request.currency(),
            "ops-sales-sim-" + simulationId + "-" + stats.attempted);

    try {
      var preparation =
          TchContextScope.runWithContextResult(
              sellerContext,
              () ->
                  commandBus.execute(
                      new PrepareSaleCommand(
                          DrawId.of(ticket.drawId()),
                          draw.drawChannelId(),
                          currency,
                          List.of(ticket.line()),
                          SaleCommunicationOptions.none())));
      stats.prepared++;
      var result =
          TchContextScope.runWithContextResult(
              sellerContext,
              () ->
                  commandBus.execute(
                      new ConfirmPreparedSaleCommand(
                          preparation.preparationId(),
                          idempotencyKey(simulationId, stats.attempted))));

      if (result.sale() != null && result.sale().outcome() == SellTicketOutcome.REJECTED) {
        stats.rejected(ticket, "sales.rejected");
        return;
      }

      stats.confirmed(
          ticket,
          new OpsSalesSimulationResponse.TicketResult(
              ticket.drawId(),
              ticket.sellerTerminalId(),
              ticket.kind().name(),
              ticket.selection(),
              result.preparationId(),
              result.ticketId(),
              result.sale() == null ? null : result.sale().publicCode(),
              request.stakeAmount()));
    } catch (ProblemRestException ex) {
      Integer status = ex.getProblem() == null ? null : ex.getProblem().getStatus();
      stats.failed(ticket, status, ex.getMessage());
    } catch (RuntimeException ex) {
      stats.failed(ticket, null, ex.getMessage());
    } finally {
      stats.attempted++;
    }
  }

  private Map<UUID, DrawSummary> loadDraws(
      TchRequestContext tenantContext, TenantId tenantId, List<UUID> drawIds) {
    var out = new LinkedHashMap<UUID, DrawSummary>();
    for (UUID drawUuid : drawIds) {
      var draw =
          TchContextScope.runWithContextResult(
              tenantContext, () -> queryBus.ask(new GetDrawByIdQuery(DrawId.of(drawUuid))));
      if (!tenantId.equals(draw.tenantId())) {
        throw ProblemRest.badRequest("ops.sales_simulation.draw_not_in_tenant: " + drawUuid);
      }
      out.put(drawUuid, draw);
    }
    return out;
  }

  private Map<UUID, SellerTerminalView> loadSellers(
      TchRequestContext tenantContext, TenantId tenantId, List<UUID> sellerTerminalIds) {
    var out = new LinkedHashMap<UUID, SellerTerminalView>();
    for (UUID sellerUuid : sellerTerminalIds) {
      var seller =
          TchContextScope.runWithContextResult(
              tenantContext,
              () ->
                  queryBus.ask(
                      new GetSellerTerminalQuery(tenantId, SellerTerminalId.of(sellerUuid))));
      out.put(sellerUuid, seller);
    }
    return out;
  }

  private OpsSalesSimulationRequest normalize(OpsSalesSimulationRequest request) {
    var mix =
        request.ticketMix() == null
            ? new OpsSalesSimulationRequest.TicketMix(100, 100, 100, 100, 100)
            : request.ticketMix();
    var currency =
        request.currency() == null || request.currency().isBlank()
            ? CommonConstants.DEFAULT_CURRENCY
            : request.currency().trim().toUpperCase(Locale.ROOT);
    var stake = request.stakeAmount() == null ? new BigDecimal("1.00") : request.stakeAmount();
    var dryRun = request.dryRun() == null || request.dryRun();
    return new OpsSalesSimulationRequest(
        request.tenantId(),
        List.copyOf(request.drawIds()),
        List.copyOf(request.sellerTerminalIds()),
        mix,
        currency,
        stake,
        request.seed(),
        dryRun,
        request.maxTickets(),
        request.reason());
  }

  private TchRequestContext tenantContext(UUID tenantId, String currency, String requestId) {
    return new TchRequestContext(
        "ops",
        tenantId,
        "ops",
        tenantId,
        currentUser(),
        java.util.Set.of(TchRole.SUPER_ADMIN),
        java.util.Set.of(),
        Locale.FRENCH,
        requestId,
        "127.0.0.1",
        "ops-sales-simulation",
        true,
        "ops-sales-simulation",
        "active",
        ApiScope.TENANT,
        null,
        TenantId.of(tenantId),
        ZoneId.of("America/Port-au-Prince"),
        Currency.getInstance(currency),
        null,
        TchActorType.APP_USER,
        null,
        java.util.Set.of(TchRole.SUPER_ADMIN.name()),
        java.util.Set.of("admin.access"),
        currentUser() == null ? null : currentUser().toString());
  }

  TchRequestContext sellerContext(
      UUID tenantId, UUID sellerTerminalId, String currency, String requestId) {
    var user = currentUser();
    return new TchRequestContext(
        "ops",
        tenantId,
        "ops",
        tenantId,
        user,
        java.util.Set.of(TchRole.SUPER_ADMIN),
        java.util.Set.of(),
        Locale.FRENCH,
        requestId,
        "127.0.0.1",
        "ops-sales-simulation",
        true,
        "ops-sales-simulation",
        "active",
        ApiScope.TENANT,
        null,
        TenantId.of(tenantId),
        ZoneId.of("America/Port-au-Prince"),
        Currency.getInstance(currency),
        null,
        TchActorType.APP_USER,
        SellerTerminalId.of(sellerTerminalId),
        java.util.Set.of(TchRole.SUPER_ADMIN.name()),
        java.util.Set.of("admin.access"),
        user == null ? null : user.toString());
  }

  String idempotencyKey(UUID simulationId, int ticketIndex) {
    return "ops-sales-sim:" + simulationId + ":" + ticketIndex;
  }

  private UUID currentUser() {
    var current = TchContext.currentOrNull();
    return current == null ? null : current.userUuid();
  }

  private static final class Stats {
    int attempted;
    int prepared;
    int confirmed;
    int rejected;
    int failed;
    BigDecimal confirmedGross = BigDecimal.ZERO;
    final List<OpsSalesSimulationResponse.TicketResult> tickets = new ArrayList<>();
    final List<OpsSalesSimulationResponse.TicketFailure> failures = new ArrayList<>();
    final Map<UUID, Counter> drawCounters = new LinkedHashMap<>();
    final Map<UUID, Counter> sellerCounters = new LinkedHashMap<>();
    final Map<SimulatedGameKind, Counter> gameCounters = new EnumMap<>(SimulatedGameKind.class);

    void addPlanned(List<OpsSalesSimulationPlanner.PlannedTicket> planned) {
      for (var ticket : planned) {
        counters(ticket).planned();
      }
    }

    void confirmed(
        OpsSalesSimulationPlanner.PlannedTicket ticket,
        OpsSalesSimulationResponse.TicketResult result) {
      confirmed++;
      confirmedGross = confirmedGross.add(result.amount());
      tickets.add(result);
      counters(ticket).confirmed();
    }

    void rejected(OpsSalesSimulationPlanner.PlannedTicket ticket, String message) {
      rejected++;
      failures.add(
          new OpsSalesSimulationResponse.TicketFailure(
              ticket.drawId(),
              ticket.sellerTerminalId(),
              ticket.kind().name(),
              ticket.selection(),
              null,
              message));
      counters(ticket).rejected();
    }

    void failed(OpsSalesSimulationPlanner.PlannedTicket ticket, Integer status, String message) {
      failed++;
      failures.add(
          new OpsSalesSimulationResponse.TicketFailure(
              ticket.drawId(),
              ticket.sellerTerminalId(),
              ticket.kind().name(),
              ticket.selection(),
              status,
              message));
      counters(ticket).failed();
    }

    private CounterGroup counters(OpsSalesSimulationPlanner.PlannedTicket ticket) {
      var draw = drawCounters.computeIfAbsent(ticket.drawId(), ignored -> new Counter());
      var seller =
          sellerCounters.computeIfAbsent(ticket.sellerTerminalId(), ignored -> new Counter());
      var game = gameCounters.computeIfAbsent(ticket.kind(), ignored -> new Counter());
      return new CounterGroup(draw, seller, game);
    }

    OpsSalesSimulationResponse toResponse(
        UUID simulationId,
        OpsSalesSimulationRequest request,
        long seed,
        int requestedTickets,
        int plannedTickets,
        Map<UUID, DrawSummary> draws,
        Map<UUID, SellerTerminalView> sellers) {
      var drawRows =
          draws.values().stream()
              .map(
                  draw -> {
                    var c = drawCounters.getOrDefault(draw.drawId().value(), new Counter());
                    return new OpsSalesSimulationResponse.DrawSummary(
                        draw.drawId().value(),
                        draw.drawChannelCode(),
                        draw.status().name(),
                        c.planned,
                        c.confirmed,
                        c.rejected,
                        c.failed);
                  })
              .toList();
      var sellerRows =
          sellers.values().stream()
              .map(
                  seller -> {
                    var c = sellerCounters.getOrDefault(seller.id().value(), new Counter());
                    return new OpsSalesSimulationResponse.SellerSummary(
                        seller.id().value(),
                        seller.terminalCode(),
                        seller.displayName(),
                        c.planned,
                        c.confirmed,
                        c.rejected,
                        c.failed);
                  })
              .toList();
      var gameRows =
          java.util.Arrays.stream(SimulatedGameKind.values())
              .map(
                  kind -> {
                    var c = gameCounters.getOrDefault(kind, new Counter());
                    return new OpsSalesSimulationResponse.GameSummary(
                        kind.name(),
                        kind.gameCode().name(),
                        kind.betType().name(),
                        kind.betOption(),
                        c.planned,
                        c.confirmed,
                        c.rejected,
                        c.failed);
                  })
              .toList();
      return new OpsSalesSimulationResponse(
          simulationId,
          request.tenantId(),
          request.dryRun(),
          request.currency(),
          request.stakeAmount(),
          seed,
          requestedTickets,
          plannedTickets,
          prepared,
          confirmed,
          rejected,
          failed,
          confirmedGross,
          drawRows,
          sellerRows,
          gameRows,
          tickets,
          failures,
          List.of(
              "Use POST /platform/ops/draw-results/manual or /override to enter results.",
              "Use POST /platform/ops/draws/apply to apply results after manual/override input."));
    }
  }

  private static class Counter {
    int planned;
    int confirmed;
    int rejected;
    int failed;
  }

  private static final class CounterGroup {
    private final Counter draw;
    private final Counter seller;
    private final Counter game;

    CounterGroup(Counter draw, Counter seller, Counter game) {
      this.draw = draw;
      this.seller = seller;
      this.game = game;
    }

    void confirmed() {
      draw.confirmed++;
      seller.confirmed++;
      game.confirmed++;
    }

    void rejected() {
      draw.rejected++;
      seller.rejected++;
      game.rejected++;
    }

    void failed() {
      draw.failed++;
      seller.failed++;
      game.failed++;
    }

    void planned() {
      draw.planned++;
      seller.planned++;
      game.planned++;
    }
  }
}
