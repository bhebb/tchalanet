package com.tchalanet.server.features.cashier.home.app;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.operational.OperationalContextHint;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.payout.api.query.ListPayoutsQuery;
import com.tchalanet.server.core.payout.internal.domain.model.PayoutClaimStatus;
import com.tchalanet.server.core.session.api.query.GetCashierSessionSummaryQuery;
import com.tchalanet.server.features.cashier.home.model.CashierAttentionLevel;
import com.tchalanet.server.features.cashier.home.model.CashierBadge;
import com.tchalanet.server.features.cashier.draws.CashierAvailableDrawView;
import com.tchalanet.server.features.cashier.draws.CashierDrawsService;
import com.tchalanet.server.features.cashier.home.model.CashierHomeDrawSummary;
import com.tchalanet.server.features.cashier.home.model.CashierHomeOperationalContext;
import com.tchalanet.server.features.cashier.home.model.CashierHomeResponse;
import com.tchalanet.server.features.cashier.home.model.CashierHomeSessionSummary;
import com.tchalanet.server.features.cashier.home.model.CashierNotification;
import com.tchalanet.server.features.cashier.home.model.CashierReadinessBlocker;
import com.tchalanet.server.features.cashier.home.model.CashierReadinessResponse;
import com.tchalanet.server.features.cashier.home.model.HomeAction;
import com.tchalanet.server.features.cashier.home.model.HomeHeader;
import com.tchalanet.server.features.cashier.home.model.HomeNavigationItem;
import com.tchalanet.server.features.cashier.home.model.HomeRequiredStep;
import com.tchalanet.server.features.cashier.home.model.HomeRequiredStepType;
import com.tchalanet.server.features.cashier.home.model.HomeWidget;
import com.tchalanet.server.platform.identity.api.model.surface.ClientSurface;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CashierHomeService {

  private static final String VERSION = "home.v1";
  private static final DateTimeFormatter TIME_LABEL = DateTimeFormatter.ofPattern("HH:mm");
  private static final DateTimeFormatter DATE_TIME_LABEL =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  private final ClientSurfaceResolver surfaceResolver;
  private final CashierDrawsService drawsService;
  private final QueryBus queryBus;

  public CashierHomeResponse mobileHome(TchRequestContext ctx, String requestedSurface) {
    var surface = surfaceResolver.resolve(ctx, requestedSurface);
    if (surface != ClientSurface.MOBILE_POS) {
      throw ProblemRest.forbidden("surface.not_allowed");
    }

    var operational = operationalContext(ctx);
    if (!operational.ready() || !operational.trusted()) {
      return missingOperationalContextHome(surface, operational);
    }

    var session = sessionSummary(ctx);
    if (session == null || !session.open()) {
      return closedSessionHome(surface, operational);
    }

    var primaryDraw = primaryDraw(ctx);
    return new CashierHomeResponse(
        surface,
        VERSION,
        HomeHeader.of("Bonjour", subtitle(operational)),
        null,
        operational,
        session,
        primaryDraw,
        new HomeAction("SELL_TICKET", "Vendre un ticket", primaryDraw != null, "/sell"),
        List.of(
            new HomeAction("RECENT_TICKETS", "Tickets récents", true, "/tickets"),
            new HomeAction("SESSION", "Session", true, "/session"),
            new HomeAction("PROFILE", "Profil", true, "/profile")),
        List.of(
            HomeWidget.untitled(
                "session_status",
                "POS_SESSION_STATUS",
                Map.of(
                    "open", session.open(),
                    "ticketCount", session.ticketCount(),
                    "salesTotal", value(session.salesTotal()))),
            HomeWidget.untitled(
                "primary_draw",
                "POS_DRAW_STATUS",
                Map.of(
                    "label", primaryDraw != null ? value(primaryDraw.label()) : "",
                    "cutoffLabel", primaryDraw != null ? value(primaryDraw.cutoffLabel()) : "",
                    "status", primaryDraw != null ? value(primaryDraw.status()) : ""))),
        mobileNavigation(),
        List.of());
  }

  public CashierReadinessResponse readiness(TchRequestContext ctx) {
    var operational = operationalContext(ctx);
    var blockers = new ArrayList<CashierReadinessBlocker>();
    if (!operational.ready() || !operational.trusted()) {
      blockers.add(new CashierReadinessBlocker(
          "OPERATIONAL_CONTEXT",
          "pos.readiness.operational_context.title",
          "pos.readiness.operational_context.message",
          Map.of("missing", operational.missing())));
    }

    var session = sessionSummary(ctx);
    if (session == null || !session.open()) {
      blockers.add(new CashierReadinessBlocker(
          "SESSION_CLOSED",
          "pos.readiness.session_closed.title",
          "pos.readiness.session_closed.message",
          Map.of()));
    }

    var badges = new ArrayList<CashierBadge>();
    var notifications = new ArrayList<CashierNotification>();
    var previousUnpaidCount = previousUnpaidPayoutCount(ctx);
    if (previousUnpaidCount > 0) {
      var params = Map.<String, Object>of("count", previousUnpaidCount);
      badges.add(new CashierBadge(
          "PREVIOUS_UNPAID_PAYOUTS",
          CashierAttentionLevel.BADGE,
          "pos.notification.previous_unpaid_payouts.title",
          params));
      notifications.add(new CashierNotification(
          "PREVIOUS_UNPAID_PAYOUTS",
          CashierAttentionLevel.CARD,
          "pos.notification.previous_unpaid_payouts.title",
          "pos.notification.previous_unpaid_payouts.message",
          "VIEW_PAYOUTS_TO_PROCESS",
          "pos.notification.previous_unpaid_payouts.action",
          params));
    }

    var ready = blockers.isEmpty();
    return new CashierReadinessResponse(
        ready,
        !ready ? CashierAttentionLevel.BLOCKED
            : notifications.isEmpty() ? CashierAttentionLevel.NONE : CashierAttentionLevel.CARD,
        badges,
        notifications,
        blockers);
  }

  private CashierHomeResponse missingOperationalContextHome(
      ClientSurface surface, CashierHomeOperationalContext operational) {
    return new CashierHomeResponse(
        surface,
        VERSION,
        HomeHeader.of("Configurer le poste", "Sélectionnez le point de vente et le terminal"),
        new HomeRequiredStep(
            HomeRequiredStepType.SELECT_OPERATIONAL_CONTEXT,
            "Configurer le poste",
            "Sélectionnez le point de vente et le terminal avant de vendre."),
        operational,
        null,
        null,
        new HomeAction(
            "SELECT_OPERATIONAL_CONTEXT",
            "Configurer le poste",
            true,
            "/operational-context/select"),
        List.of(),
        List.of(),
        List.of(new HomeNavigationItem("profile", "Profil", "/profile")),
        List.of());
  }

  private CashierHomeResponse closedSessionHome(
      ClientSurface surface, CashierHomeOperationalContext operational) {
    return new CashierHomeResponse(
        surface,
        VERSION,
        HomeHeader.of("Session fermée", subtitle(operational)),
        new HomeRequiredStep(
            HomeRequiredStepType.OPEN_SESSION,
            "Session fermée",
            "Ouvrez une session pour commencer à vendre."),
        operational,
        new CashierHomeSessionSummary(false, null, null, 0, null),
        null,
        new HomeAction("OPEN_SESSION", "Ouvrir session", true, "/session/open"),
        List.of(new HomeAction("PROFILE", "Profil", true, "/profile")),
        List.of(),
        List.of(new HomeNavigationItem("profile", "Profil", "/profile")),
        List.of());
  }

  private CashierHomeOperationalContext operationalContext(TchRequestContext ctx) {
    OperationalContextHint hint = ctx.operationalContext();
    var missing = new ArrayList<String>();
    if (hint == null || hint.outletId() == null) {
      missing.add("OUTLET");
    }
    if (hint == null || hint.terminalId() == null) {
      missing.add("TERMINAL");
    }
    boolean trusted = hint != null && hint.trustedForSensitiveOperation();
    boolean ready = missing.isEmpty() && trusted;
    return new CashierHomeOperationalContext(
        ready,
        trusted,
        hint != null && hint.source() != null ? hint.source().name() : null,
        hint != null ? hint.outletId() : null,
        hint != null && hint.outletId() != null ? "PDV " + shortId(hint.outletId().value()) : null,
        hint != null ? hint.terminalId() : null,
        hint != null && hint.terminalId() != null ? "TCH-POS-" + shortId(hint.terminalId().value()) : null,
        hint != null ? hint.salesSessionId() : null,
        missing);
  }

  private CashierHomeSessionSummary sessionSummary(TchRequestContext ctx) {
    var view =
        queryBus.ask(
            new GetCashierSessionSummaryQuery(
                ctx.effectiveTenantIdRequired(), ctx.currentUserIdRequired()));
    if (view == null || !view.active()) {
      return new CashierHomeSessionSummary(false, null, null, 0, money(0, ctx));
    }
    return new CashierHomeSessionSummary(
        true,
        view.openedAt(),
        instantLabel(view.openedAt(), ctx.tenantZoneId(), TIME_LABEL),
        view.ticketCount(),
        money(view.salesTotalCents(), ctx));
  }

  private long previousUnpaidPayoutCount(TchRequestContext ctx) {
    var zone = ctx.tenantZoneId() == null ? ZoneId.of("UTC") : ctx.tenantZoneId();
    var todayStart = java.time.LocalDate.now(zone).atStartOfDay(zone).toInstant();
    return oldPayoutCount(PayoutClaimStatus.OPEN, todayStart);
  }

  private long oldPayoutCount(PayoutClaimStatus status, Instant before) {
    return queryBus.ask(new ListPayoutsQuery(
        status,
        null,
        null,
        null,
        null,
        before,
        PageRequest.of(0, 1)
    )).totalElements();
  }

  private CashierHomeDrawSummary primaryDraw(TchRequestContext ctx) {
    List<CashierAvailableDrawView> draws = drawsService.listAvailable(ctx, 24, 1);
    if (draws.isEmpty()) {
      return null;
    }
    var draw = draws.get(0);
    return new CashierHomeDrawSummary(
        draw.drawId(),
        draw.drawChannelId(),
        draw.channelLabel(),
        draw.scheduledAt(),
        instantLabel(draw.scheduledAt(), ctx.tenantZoneId(), DATE_TIME_LABEL),
        draw.cutoffAt(),
        cutoffLabel(draw.cutoffAt()),
        draw.status());
  }


  private List<HomeNavigationItem> mobileNavigation() {
    return List.of(
        new HomeNavigationItem("sell", "Vendre", "/sell"),
        new HomeNavigationItem("tickets", "Tickets", "/tickets"),
        new HomeNavigationItem("session", "Session", "/session"),
        new HomeNavigationItem("profile", "Profil", "/profile"));
  }

  private String instantLabel(Instant instant, ZoneId zone, DateTimeFormatter formatter) {
    if (instant == null) {
      return null;
    }
    return formatter.withZone(zone != null ? zone : ZoneId.of("UTC")).format(instant);
  }

  private String cutoffLabel(Instant cutoffAt) {
    if (cutoffAt == null) {
      return null;
    }
    long minutes = Duration.between(Instant.now(), cutoffAt).toMinutes();
    return minutes > 0 ? "Clôture dans " + minutes + " min" : "Clôture imminente";
  }

  private String money(long cents, TchRequestContext ctx) {
    String currency = ctx.tenantCurrency() != null ? ctx.tenantCurrency().getCurrencyCode() : "HTG";
    return BigDecimal.valueOf(cents, 2).setScale(2, RoundingMode.HALF_UP) + " " + currency;
  }

  private String shortId(java.util.UUID value) {
    return value == null
        ? ""
        : value.toString().substring(0, 8).toUpperCase(java.util.Locale.ROOT);
  }

  private String subtitle(CashierHomeOperationalContext operational) {
    if (operational.outletName() == null && operational.terminalLabel() == null) {
      return null;
    }
    if (operational.outletName() == null) {
      return operational.terminalLabel();
    }
    if (operational.terminalLabel() == null) {
      return operational.outletName();
    }
    return value(operational.outletName()) + " • " + value(operational.terminalLabel());
  }

  private static String value(String value) {
    return value == null ? "" : value;
  }
}
