package com.tchalanet.server.features.cashier.home.app;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchActorType;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalStatus;
import com.tchalanet.server.core.sellerterminal.api.query.GetSellerTerminalQuery;
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
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CashierHomeService {

  private static final String VERSION = "home.v1";
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

    // Resolve tenant currency once — used in all response paths so that the
    // mobile never needs to hardcode a currency code.
    var currency = ctx.tenantCurrency() != null
        ? ctx.tenantCurrency().getCurrencyCode()
        : "HTG";

    if (ctx.actorType() == TchActorType.SELLER_TERMINAL) {
      return sellerTerminalHome(ctx, surface, currency);
    }

    throw ProblemRest.forbidden("seller_terminal.actor_required");
  }

  public CashierReadinessResponse readiness(TchRequestContext ctx) {
    var sellerTerminalActor = ctx != null && ctx.actorType() == TchActorType.SELLER_TERMINAL
        && ctx.sellerTerminalId() != null;
    var blockers = sellerTerminalActor
        ? List.<CashierReadinessBlocker>of()
        : List.of(new CashierReadinessBlocker(
          "OPERATIONAL_CONTEXT",
          "pos.readiness.operational_context.title",
          "pos.readiness.operational_context.message",
          Map.of("missing", List.of("SELLER_TERMINAL"))));

    var badges = new ArrayList<CashierBadge>();
    var notifications = new ArrayList<CashierNotification>();

    var ready = blockers.isEmpty();
    return new CashierReadinessResponse(
        ready,
        !ready ? CashierAttentionLevel.BLOCKED
            : notifications.isEmpty() ? CashierAttentionLevel.NONE : CashierAttentionLevel.CARD,
        badges,
        notifications,
        blockers);
  }

  private CashierHomeResponse sellerTerminalHome(
      TchRequestContext ctx, ClientSurface surface, String currency) {
    var sellerTerminalId = ctx.sellerTerminalIdRequired();
    var terminal = queryBus.ask(new GetSellerTerminalQuery(
        ctx.effectiveTenantIdRequired(), sellerTerminalId));
    var canSell = terminal.status() == SellerTerminalStatus.ACTIVE;
    HomeRequiredStep requiredStep = null;
    if (terminal.mustChangePin()) {
      requiredStep = new HomeRequiredStep(
          HomeRequiredStepType.MUST_CHANGE_PIN,
          "Changement de PIN requis",
          "Vous devez changer votre PIN temporaire avant de continuer.");
    }
    var primaryDraw = requiredStep != null ? null : primaryDraw(ctx);
    var operationalCtx = new CashierHomeOperationalContext(
        canSell && requiredStep == null, true, "SELLER_TERMINAL",
        sellerTerminalId,
        terminal.displayName(),
        List.of()
    );
    return new CashierHomeResponse(
        surface, VERSION,
        HomeHeader.of("Bonjour", terminal.displayName()),
        requiredStep,
        operationalCtx,
        null,
        primaryDraw,
        new HomeAction("SELL_TICKET", "Vendre un ticket", canSell && primaryDraw != null, "/sell"),
        List.of(
            new HomeAction("RECENT_TICKETS", "Tickets récents", true, "/tickets"),
            new HomeAction("PROFILE", "Profil", true, "/profile")),
        List.of(),
        mobileNavigation(),
        List.of(),
        currency
    );
  }

  private CashierHomeSessionSummary v0SessionSummary(TchRequestContext ctx) {
    return new CashierHomeSessionSummary(
        true,
        null,
        null,
        0,
        money(0, ctx));
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
}
