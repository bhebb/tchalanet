package com.tchalanet.server.core.draw.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.draw.application.port.out.DrawSummaryReaderPort;
import com.tchalanet.server.core.draw.application.query.model.DrawSearchCriteria;
import com.tchalanet.server.core.draw.application.query.model.ListLatestDrawsWithResultsQuery;
import com.tchalanet.server.core.draw.application.query.projection.DrawSummary;
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
