package com.tchalanet.server.features.pos.draws;

import com.tchalanet.server.catalog.drawchannel.api.DrawChannelCatalog;
import com.tchalanet.server.catalog.drawchannel.api.DrawChannelDisplayFormatter;
import com.tchalanet.server.catalog.drawchannel.api.model.GameSummaryView;
import com.tchalanet.server.catalog.game.api.GameCatalog;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.time.TchTimeProvider;
import com.tchalanet.server.core.draw.api.query.CashierNextDrawView;
import com.tchalanet.server.core.draw.api.query.ListCashierNextDrawsQuery;
import com.tchalanet.server.platform.tenant.api.TenantBusinessCalendarApi;
import com.tchalanet.server.platform.tenantgame.api.TenantGameApi;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PosDrawsService {

  private final QueryBus queryBus;
  private final DrawChannelCatalog drawChannelCatalog;
  private final TenantGameApi tenantGameApi;
  private final GameCatalog gameCatalog;
  private final ResultSlotCatalog resultSlotCatalog;
  private final DrawChannelDisplayFormatter drawChannelDisplayFormatter;
  private final TenantBusinessCalendarApi tenantBusinessCalendarApi;
  private final TchTimeProvider timeProvider;

  public List<PosAvailableDrawView> listAvailable(
      TchRequestContext ctx, int lookaheadHours, int limit) {
    if (!tenantBusinessOpen(ctx)) {
      return List.of();
    }

    var rows = queryBus.ask(new ListCashierNextDrawsQuery(lookaheadHours, limit));
    if (rows.isEmpty()) {
      return List.of();
    }

    var tenantId = ctx.effectiveTenantIdRequired();
    var activeGameCodes =
        gameCatalog.listActive().stream()
            .map(game -> game.code().toUpperCase())
            .collect(Collectors.toSet());
    var tenantPosGameCodes =
        tenantGameApi.listGames(tenantId).stream()
            .filter(game -> game.enabled() && game.visibleInPos())
            .map(game -> game.gameCode().toUpperCase())
            .collect(Collectors.toSet());

    var gamesByChannelCode =
        drawChannelCatalog.listChannelGames(tenantId).stream()
            .collect(
                Collectors.toMap(
                    cg -> cg.channelCode(),
                    cg ->
                        cg.games().stream()
                            .filter(GameSummaryView::enabled)
                            .map(GameSummaryView::gameCode)
                            .filter(gameCode -> activeGameCodes.contains(gameCode.toUpperCase()))
                            .filter(gameCode -> tenantPosGameCodes.contains(gameCode.toUpperCase()))
                            .toList(),
                    (a, b) -> a));

    var locale = ctx.locale() == null ? Locale.FRENCH : ctx.locale();

    return rows.stream()
        .filter(d -> !gamesByChannelCode.getOrDefault(d.channelCode(), List.of()).isEmpty())
        .map(d -> toView(d, gamesByChannelCode, locale, ctx.tenantZoneId()))
        .toList();
  }

  private PosAvailableDrawView toView(
      CashierNextDrawView d,
      Map<String, List<String>> gamesByChannelCode,
      Locale locale,
      ZoneId tenantZone) {
    var formattedLabel =
        drawChannelDisplayFormatter.resolve(d.channelLabel(), d.drawTime(), locale);
    var games = gamesByChannelCode.getOrDefault(d.channelCode(), List.of());
    var providerZone = resultSlotCatalog.requireByKey(d.resultSlotKey()).timezone();
    var providerDateTime = d.scheduledAt().atZone(providerZone);
    var localZone = tenantZone == null ? ZoneId.of("UTC") : tenantZone;
    var localDateTime = d.scheduledAt().atZone(localZone);
    return new PosAvailableDrawView(
        d.drawId(),
        d.drawChannelId(),
        d.drawDate(),
        d.resultSlotId(),
        d.resultSlotKey(),
        d.channelCode(),
        formattedLabel,
        games,
        d.status(),
        d.scheduledAt(),
        d.cutoffAt(),
        providerDateTime.toLocalDate(),
        providerDateTime.toLocalTime(),
        providerZone.getId(),
        localDateTime.toLocalDate(),
        localDateTime.toLocalTime(),
        localZone.getId());
  }

  private boolean tenantBusinessOpen(TchRequestContext ctx) {
    var tenantId = ctx.tenantIdRequired();
    var zone = ctx.tenantZoneId() == null ? ZoneId.of("UTC") : ctx.tenantZoneId();
    var businessDate = timeProvider.now().atZone(zone).toLocalDate();
    var day = tenantBusinessCalendarApi.resolveBusinessDay(tenantId, businessDate);
    return day == null || day.open();
  }
}
