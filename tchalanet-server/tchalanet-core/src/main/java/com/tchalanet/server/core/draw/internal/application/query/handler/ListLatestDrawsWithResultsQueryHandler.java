package com.tchalanet.server.core.draw.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawSummaryReaderPort;
import com.tchalanet.server.core.draw.api.query.DrawSearchCriteria;
import com.tchalanet.server.core.draw.api.query.ListLatestDrawsWithResultsQuery;
import com.tchalanet.server.core.draw.internal.application.query.projection.DrawSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class ListLatestDrawsWithResultsQueryHandler implements QueryHandler<ListLatestDrawsWithResultsQuery, TchPage<DrawSummary>> {

    private final DrawSummaryReaderPort reader;

    @Override
    public TchPage<DrawSummary> handle(ListLatestDrawsWithResultsQuery query) {
        log.debug("Listing latest draws with results for slots: {}", query.resultSlotKeys());
        var criteria = DrawSearchCriteria.forLatestWithResults(query.resultSlotKeys());
        return reader.listLatestWithResults(criteria, query.pageable());
    }
}
