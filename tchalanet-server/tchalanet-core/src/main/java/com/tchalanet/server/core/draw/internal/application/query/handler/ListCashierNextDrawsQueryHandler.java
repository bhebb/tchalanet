package com.tchalanet.server.core.draw.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.time.TchTimeProvider;
import com.tchalanet.server.core.draw.api.model.DrawStatus;
import com.tchalanet.server.core.draw.api.query.CashierNextDrawView;
import com.tchalanet.server.core.draw.api.query.DrawSearchCriteria;
import com.tchalanet.server.core.draw.api.query.ListCashierNextDrawsQuery;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawSummaryReaderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;

import java.util.List;

@UseCase
@RequiredArgsConstructor
public class ListCashierNextDrawsQueryHandler
    implements QueryHandler<ListCashierNextDrawsQuery, List<CashierNextDrawView>> {

    private static final int MAX_LOOKAHEAD_HOURS = 48;
    private static final int MAX_LIMIT = 20;

    private final DrawSummaryReaderPort reader;
    private final TchTimeProvider timeProvider;

    @Override
    public List<CashierNextDrawView> handle(ListCashierNextDrawsQuery query) {
        int lookahead = Math.min(Math.max(query.lookaheadHours(), 1), MAX_LOOKAHEAD_HOURS);
        int limit = Math.min(Math.max(query.limit(), 1), MAX_LIMIT);

        var criteria = DrawSearchCriteria.forNext(null, lookahead, limit);
        var page = reader.listNext(criteria, PageRequest.of(0, MAX_LIMIT));
        var now = timeProvider.now();

        return page.items().stream()
            .filter(d -> d.status() == DrawStatus.OPEN)
            .filter(d -> d.cutoffAt() != null && now.isBefore(d.cutoffAt()))
            .limit(limit)
            .map(d -> new CashierNextDrawView(
                d.drawId(),
                d.drawChannelId(),
                d.resultSlotId(),
                d.resultSlotKey(),
                d.drawChannelCode(),
                d.drawChannelLabel(),
                d.drawDate(),
                d.drawTime(),
                d.scheduledAt(),
                d.cutoffAt(),
                d.status().name()))
            .toList();
    }
}
