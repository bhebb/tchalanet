package com.tchalanet.server.core.drawresult.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.drawresult.internal.application.port.out.PublicDrawResultSlotReaderPort;
import com.tchalanet.server.core.drawresult.api.query.ListPublicDrawResultSlotDetailsQuery;
import com.tchalanet.server.core.drawresult.api.query.view.PublicDrawResultSlotDetailsView;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListPublicDrawResultSlotDetailsQueryHandler
    implements QueryHandler<
    ListPublicDrawResultSlotDetailsQuery, List<PublicDrawResultSlotDetailsView>> {

    private static final int DEFAULT_HISTORY_LIMIT = 12;
    private static final int MAX_HISTORY_LIMIT = 20;

    private final PublicDrawResultSlotReaderPort reader;
    private final Clock clock;

    @Override
    public List<PublicDrawResultSlotDetailsView> handle(ListPublicDrawResultSlotDetailsQuery query) {
        var provider = PublicDrawResultQueryNormalizer.provider(query.provider());

        return reader.listPublicSlotDetails(
            PublicDrawResultQueryNormalizer.slotKeys(query.slotKeys()),
            provider,
            resolveResultDate(query.resultDate()),
            normalizeHistoryLimit(query.historyLimit()));
    }

    private LocalDate resolveResultDate(LocalDate requested) {
        if (requested != null) {
            return requested;
        }

        return LocalDate.now(clock);
    }

    private static int normalizeHistoryLimit(int historyLimit) {
        if (historyLimit <= 0) {
            return DEFAULT_HISTORY_LIMIT;
        }

        return Math.min(historyLimit, MAX_HISTORY_LIMIT);
    }
}
