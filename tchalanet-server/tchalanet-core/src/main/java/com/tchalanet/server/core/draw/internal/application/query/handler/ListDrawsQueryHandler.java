package com.tchalanet.server.core.draw.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawSummaryReaderPort;
import com.tchalanet.server.core.draw.api.query.ListDrawsQuery;
import com.tchalanet.server.core.draw.internal.application.query.projection.DrawSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class ListDrawsQueryHandler implements QueryHandler<ListDrawsQuery, TchPage<DrawSummary>> {

    private final DrawSummaryReaderPort reader;

    @Override
    public TchPage<DrawSummary> handle(ListDrawsQuery query) {
        log.debug("Listing draws with criteria: {}", query.criteria());
        return reader.findByCriteria(query.criteria(), query.pageable());
    }
}
