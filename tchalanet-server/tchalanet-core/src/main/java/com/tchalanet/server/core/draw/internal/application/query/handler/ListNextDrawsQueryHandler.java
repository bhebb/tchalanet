package com.tchalanet.server.core.draw.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawSummaryReaderPort;
import com.tchalanet.server.core.draw.api.query.DrawSearchCriteria;
import com.tchalanet.server.core.draw.api.query.ListNextDrawsQuery;
import com.tchalanet.server.core.draw.api.query.DrawSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class ListNextDrawsQueryHandler implements QueryHandler<ListNextDrawsQuery, TchPage<DrawSummary>> {

    private static final int MAX_LOOKAHEAD_HOURS = 168; // 7 days
    private static final int MAX_LIMIT_PER_CHANNEL = 20;

    private final DrawSummaryReaderPort reader;

    @Override
    public TchPage<DrawSummary> handle(ListNextDrawsQuery query) {
        Objects.requireNonNull(query, "query is required");
        Objects.requireNonNull(query.pageable(), "pageable is required");

        if (query.lookaheadHours() <= 0) {
            throw new IllegalArgumentException("lookaheadHours must be > 0");
        }

        if (query.lookaheadHours() > MAX_LOOKAHEAD_HOURS) {
            throw new IllegalArgumentException("lookaheadHours must be <= " + MAX_LOOKAHEAD_HOURS);
        }

        if (query.limitPerChannel() <= 0) {
            throw new IllegalArgumentException("limitPerChannel must be > 0");
        }

        if (query.limitPerChannel() > MAX_LIMIT_PER_CHANNEL) {
            throw new IllegalArgumentException("limitPerChannel must be <= " + MAX_LIMIT_PER_CHANNEL);
        }

        log.debug("Listing next draws for slot: {}, lookahead: {}h, limit: {}",
            query.resultSlotId(), query.lookaheadHours(), query.limitPerChannel());

        var criteria = DrawSearchCriteria.forNext(
            query.resultSlotId(),
            query.lookaheadHours(),
            query.limitPerChannel()
        );
        return reader.listNext(criteria, query.pageable());
    }
}
