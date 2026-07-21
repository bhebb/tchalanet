package com.tchalanet.server.features.pos.tickets.app;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.web.advice.ApiResponseNotices;
import com.tchalanet.server.common.web.api.NoticeSeverity;
import com.tchalanet.server.common.web.api.NoticeSource;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.analytics.api.model.AnalyticsTrustScope;
import com.tchalanet.server.core.analytics.api.model.AnalyticsTrustState;
import com.tchalanet.server.core.analytics.api.query.GetAnalyticsTrustStateQuery;
import com.tchalanet.server.core.analytics.api.query.GetCashierDashboardStatsQuery;
import com.tchalanet.server.core.sales.api.command.cancel.CancelTicketCommand;
import com.tchalanet.server.core.sales.api.model.verification.CustomerTicketStatus;
import com.tchalanet.server.core.sales.api.model.verification.TicketCashierVerificationView;
import com.tchalanet.server.core.sales.api.query.GetTicketForCashierVerificationQuery;
import com.tchalanet.server.core.sales.api.query.GetTicketPrintViewQuery;
import com.tchalanet.server.core.sales.api.query.ListTicketsQuery;
import com.tchalanet.server.features.pos.error.PosErrorCodes;
import com.tchalanet.server.features.pos.tickets.mapper.PosTicketMapper;
import com.tchalanet.server.features.pos.tickets.model.DrawStatLineItem;
import com.tchalanet.server.features.pos.tickets.model.PosAction;
import com.tchalanet.server.features.pos.tickets.model.PosActionType;
import com.tchalanet.server.features.pos.tickets.model.PosTicketCancelRequest;
import com.tchalanet.server.features.pos.tickets.model.PosTicketCancelResponse;
import com.tchalanet.server.features.pos.tickets.model.PosTicketDetailsResponse;
import com.tchalanet.server.features.pos.tickets.model.PosTicketPageResponse;
import com.tchalanet.server.features.pos.tickets.model.PosTicketVerificationResponse;
import com.tchalanet.server.features.pos.tickets.model.PosTicketVerificationSeverity;
import com.tchalanet.server.features.pos.tickets.model.PosTicketVerificationStatus;
import com.tchalanet.server.features.pos.tickets.model.PosVerifyTicketRequest;
import com.tchalanet.server.features.pos.tickets.model.SellerTerminalDailyStatsResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PosTicketsService {

  private final QueryBus queryBus;
  private final CommandBus commandBus;
  private final PosTicketMapper mapper;
  private final TicketScanResolver ticketScanResolver;

  public PosTicketCancelResponse cancel(
      TchRequestContext ctx, TicketId ticketId, PosTicketCancelRequest request) {
    validateSellerContext(ctx, request.sellerTerminalId());
    var result = commandBus.execute(new CancelTicketCommand(ticketId, request.reason()));
    return new PosTicketCancelResponse(
        result.ticketId(), result.outcome(), result.cancelledAt(), result.issues());
  }

  public TchPage<PosTicketPageResponse> listTickets(
      SellerTerminalId sellerTerminalId,
      DrawId drawId,
      String status,
      String q,
      Instant from,
      Instant to,
      Pageable pageable) {
    var result =
        queryBus.ask(
            new ListTicketsQuery(
                sellerTerminalId, drawId, status, q, from, to, new TchPageRequest(pageable)));
    return TchPageMapper.map(result, mapper::toPageResponse);
  }

  public SellerTerminalDailyStatsResponse sellerTerminalStats(TchRequestContext ctx, String date) {
    var zone = ctx.tenantZoneId() != null ? ctx.tenantZoneId() : ZoneId.of("UTC");
    var targetDate = date != null ? LocalDate.parse(date) : LocalDate.now(zone);
    var currency = ctx.tenantCurrency() != null ? ctx.tenantCurrency().getCurrencyCode() : "HTG";
    var trust =
        queryBus.ask(
            new GetAnalyticsTrustStateQuery(
                AnalyticsTrustScope.sellerTerminal(
                    ctx.effectiveTenantIdRequired(),
                    ctx.sellerTerminalIdRequired(),
                    targetDate,
                    targetDate)));
    if (trust == null || trust.state() != AnalyticsTrustState.READY) {
      ApiResponseNotices.degradation(
          PosErrorCodes.DASHBOARD_ANALYTICS_UNAVAILABLE.code(),
          "features.pos",
          NoticeSeverity.WARN,
          NoticeSource.of("sellerTerminalStats").operation("verifyAnalyticsTrust"),
          null,
          Map.of("reasonCode", trust == null ? "analytics.trust.unknown" : trust.reasonCode()),
          "seller_terminal.stats");
      return SellerTerminalDailyStatsResponse.unavailable(currency, trust);
    }
    var stats =
        queryBus.ask(
            new GetCashierDashboardStatsQuery(
                ctx.effectiveTenantIdRequired(), ctx.sellerTerminalIdRequired(), targetDate));
    var breakdown =
        stats.drawBreakdown().stream()
            .map(
                b ->
                    new DrawStatLineItem(
                        b.drawId().toString(),
                        b.drawChannelCode(),
                        b.ticketsSold(),
                        toCents(b.grossSales())))
            .toList();
    return new SellerTerminalDailyStatsResponse(
        true,
        trust.state().name(),
        trust.reasonCode(),
        stats.today().ticketsSold(),
        toCents(stats.today().grossSales()),
        toCents(stats.today().sellerCommission()),
        currency,
        breakdown);
  }

  private static long toCents(java.math.BigDecimal amount) {
    return amount == null ? 0L : amount.movePointRight(2).longValueExact();
  }

  public PosTicketDetailsResponse getDetails(TicketId ticketId) {
    // Use the print view — it includes publicCode, drawChannelName, seller context,
    // bet lines (with promo flags), and charges. Richer than TicketDetailsView.
    var printView = queryBus.ask(new GetTicketPrintViewQuery(ticketId));
    return mapper.toDetailsResponse(printView);
  }

  public PosTicketVerificationResponse verify(
      TchRequestContext ctx, PosVerifyTicketRequest request) {
    if (ctx == null || ctx.sellerTerminalId() == null) {
      return response(
          PosTicketVerificationStatus.OPERATION_NOT_ALLOWED,
          PosTicketVerificationSeverity.ERROR,
          null,
          "pos.ticket.verify.operation_not_allowed.title",
          "pos.ticket.verify.operation_not_allowed.message",
          Map.of(),
          List.of(
              PosAction.enabled(
                  PosActionType.CONTACT_ADMIN, "pos.action.contact_admin", Map.of())));
    }

    var publicCode = ticketScanResolver.resolvePublicCode(request.scannedValue());
    try {
      var ticket = queryBus.ask(new GetTicketForCashierVerificationQuery(publicCode));
      return verificationResponse(ticket);
    } catch (ProblemRestException ex) {
      if (!isTicketNotFound(ex)) {
        throw ex;
      }
      return response(
          PosTicketVerificationStatus.NOT_FOUND,
          PosTicketVerificationSeverity.ERROR,
          null,
          "pos.ticket.verify.not_found.title",
          "pos.ticket.verify.not_found.message",
          Map.of("publicCode", publicCode),
          List.of(PosAction.enabled(PosActionType.NONE, "pos.action.none", Map.of())));
    }
  }

  private boolean isTicketNotFound(ProblemRestException ex) {
    // Legacy problems may carry no properties at all — never NPE on them.
    var properties = ex.getProblem().getProperties();
    return properties != null && "ticket.not_found".equals(properties.get("code"));
  }

  private PosTicketVerificationResponse verificationResponse(TicketCashierVerificationView ticket) {
    var baseParams = params(ticket);
    if (ticket.customerStatus() == CustomerTicketStatus.CANCELLED) {
      return status(
          ticket,
          PosTicketVerificationStatus.CANCELLED,
          PosTicketVerificationSeverity.ERROR,
          baseParams);
    }
    if (ticket.customerStatus() == CustomerTicketStatus.VOIDED) {
      return status(
          ticket,
          PosTicketVerificationStatus.VOIDED,
          PosTicketVerificationSeverity.ERROR,
          baseParams);
    }
    if (ticket.customerStatus() == CustomerTicketStatus.AWAITING_RESULT) {
      var pendingStatus =
          ticket.drawScheduledAt() != null && ticket.drawScheduledAt().isAfter(Instant.now())
              ? PosTicketVerificationStatus.NOT_PAYABLE_PENDING_DRAW
              : PosTicketVerificationStatus.NOT_PAYABLE_RESULT_PENDING;
      return status(ticket, pendingStatus, PosTicketVerificationSeverity.INFO, baseParams);
    }
    if (ticket.customerStatus() == CustomerTicketStatus.LOST) {
      return status(
          ticket,
          PosTicketVerificationStatus.NOT_PAYABLE_LOST,
          PosTicketVerificationSeverity.INFO,
          baseParams);
    }
    if (ticket.customerStatus() == CustomerTicketStatus.WON_PAID) {
      return status(
          ticket,
          PosTicketVerificationStatus.ALREADY_PAID,
          PosTicketVerificationSeverity.SUCCESS,
          baseParams);
    }
    if (ticket.customerStatus() == CustomerTicketStatus.CORRECTED) {
      return status(
          ticket,
          PosTicketVerificationStatus.REPAIR_REQUIRED,
          PosTicketVerificationSeverity.WARNING,
          baseParams);
    }
    if (ticket.customerStatus() == CustomerTicketStatus.WON_CLAIMABLE) {
      return status(
          ticket,
          PosTicketVerificationStatus.PAYABLE,
          PosTicketVerificationSeverity.SUCCESS,
          baseParams);
    }
    return status(
        ticket,
        PosTicketVerificationStatus.BLOCKED,
        PosTicketVerificationSeverity.WARNING,
        baseParams);
  }

  private PosTicketVerificationResponse status(
      TicketCashierVerificationView ticket,
      PosTicketVerificationStatus status,
      PosTicketVerificationSeverity severity,
      Map<String, Object> params) {
    return response(
        status,
        severity,
        ticket.ticketId().value(),
        "pos.ticket.verify." + key(status) + ".title",
        "pos.ticket.verify." + key(status) + ".message",
        params,
        actions(ticket, status));
  }

  private PosTicketVerificationResponse response(
      PosTicketVerificationStatus status,
      PosTicketVerificationSeverity severity,
      UUID ticketId,
      String titleKey,
      String messageKey,
      Map<String, Object> params,
      List<PosAction> actions) {
    return new PosTicketVerificationResponse(
        status.name(), severity.name(), ticketId, titleKey, messageKey, params, actions);
  }

  private List<PosAction> actions(
      TicketCashierVerificationView ticket, PosTicketVerificationStatus status) {
    if (status == PosTicketVerificationStatus.NOT_FOUND) {
      return List.of(PosAction.enabled(PosActionType.NONE, "pos.action.none", Map.of()));
    }
    return List.of(
        PosAction.enabled(
            PosActionType.VIEW_TICKET,
            "pos.action.view_ticket",
            Map.of("publicCode", ticket.publicCode())),
        PosAction.enabled(
            PosActionType.REPRINT_TICKET,
            "pos.action.reprint_ticket",
            Map.of("publicCode", ticket.publicCode())),
        PosAction.enabled(
            PosActionType.RESEND_TICKET,
            "pos.action.resend_ticket",
            Map.of("publicCode", ticket.publicCode())));
  }

  private Map<String, Object> params(TicketCashierVerificationView ticket) {
    var params = new LinkedHashMap<String, Object>();
    params.put("publicCode", ticket.publicCode());
    params.put("displayCode", ticket.displayCode());
    params.put("ticketCode", ticket.ticketCode());
    params.put("ticketStatus", ticket.customerStatus().name());
    if (ticket.winningAmount() != null) {
      params.put("amount", ticket.winningAmount().amount().toPlainString());
      params.put("currency", ticket.winningAmount().currency().value());
    }
    return params;
  }

  private String key(PosTicketVerificationStatus status) {
    return status.name().toLowerCase(java.util.Locale.ROOT);
  }

  private void validateSellerContext(TchRequestContext ctx, java.util.UUID sellerTerminalId) {
    if (ctx == null) {
      throw ProblemRest.of(PosErrorCodes.SELLER_TERMINAL_REQUIRED);
    }
    var currentSellerTerminalId = ctx.sellerTerminalIdRequired();
    if (sellerTerminalId != null && !sellerTerminalId.equals(currentSellerTerminalId.value())) {
      throw ProblemRest.of(PosErrorCodes.SELLER_TERMINAL_MISMATCH);
    }
  }
}
