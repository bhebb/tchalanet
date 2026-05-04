package com.tchalanet.server.core.draw.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.draw.application.port.out.DrawSummaryReaderPort;
import com.tchalanet.server.core.draw.application.query.model.DrawSearchCriteria;
import com.tchalanet.server.core.draw.application.query.model.ListNextDrawsQuery;
import com.tchalanet.server.core.draw.domain.model.DrawSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class ListNextDrawsHandler implements QueryHandler<ListNextDrawsQuery, TchPage<DrawSummary>> {

    private final DrawSummaryReaderPort reader;

    @Override
    public TchPage<DrawSummary> handle(ListNextDrawsQuery query) {
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
