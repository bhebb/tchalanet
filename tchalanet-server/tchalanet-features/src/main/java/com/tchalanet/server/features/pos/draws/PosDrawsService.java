package com.tchalanet.server.features.pos.draws;

import com.tchalanet.server.catalog.drawchannel.api.DrawChannelCatalog;
import com.tchalanet.server.catalog.drawchannel.api.DrawChannelDisplayFormatter;
import com.tchalanet.server.catalog.drawchannel.api.model.GameSummaryView;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.draw.api.query.CashierNextDrawView;
import com.tchalanet.server.core.draw.api.query.ListCashierNextDrawsQuery;
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
    private final DrawChannelDisplayFormatter drawChannelDisplayFormatter;

    public List<PosAvailableDrawView> listAvailable(
        TchRequestContext ctx,
        int lookaheadHours,
        int limit
    ) {
        var rows = queryBus.ask(new ListCashierNextDrawsQuery(lookaheadHours, limit));
        if (rows.isEmpty()) {
            return List.of();
        }

        var gamesByChannelCode = drawChannelCatalog.listChannelGames(ctx.effectiveTenantIdRequired())
            .stream()
            .collect(Collectors.toMap(
                cg -> cg.channelCode(),
                cg -> cg.games().stream().map(GameSummaryView::gameCode).toList(),
                (a, b) -> a
            ));

        var locale = ctx.locale() == null ? Locale.FRENCH : ctx.locale();

        return rows.stream()
            .map(d -> toView(d, gamesByChannelCode, locale))
            .toList();
    }

    private PosAvailableDrawView toView(
        CashierNextDrawView d,
        Map<String, List<String>> gamesByChannelCode,
        Locale locale
    ) {
        var formattedLabel = drawChannelDisplayFormatter.resolve(d.channelLabel(), d.drawTime(), locale);
        var games = gamesByChannelCode.getOrDefault(d.channelCode(), List.of());
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
            d.cutoffAt()
        );
    }
}
