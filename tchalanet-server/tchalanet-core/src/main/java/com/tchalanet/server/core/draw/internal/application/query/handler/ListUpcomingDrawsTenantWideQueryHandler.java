package com.tchalanet.server.core.draw.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.api.query.DrawSearchCriteria;
import com.tchalanet.server.core.draw.api.query.ListUpcomingDrawsTenantWideQuery;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawSummaryReaderPort;
import com.tchalanet.server.core.draw.api.query.DrawSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;

import java.util.List;

/**
 * Returns the next upcoming draws for the current tenant across all channels.
 * Used notably by {@code core.offlinesync} to embed a list of allowed draws in a fresh grant.
 */
@UseCase
@RequiredArgsConstructor
public class ListUpcomingDrawsTenantWideQueryHandler
    implements QueryHandler<ListUpcomingDrawsTenantWideQuery, List<DrawSummary>> {

    private final DrawSummaryReaderPort reader;

    @Override
    @TchTx(readOnly = true)
    public List<DrawSummary> handle(ListUpcomingDrawsTenantWideQuery query) {
        int lookahead = Math.max(1, query.lookaheadHours());
        int limit = Math.max(1, Math.min(query.limit(), 200));
        var criteria = DrawSearchCriteria.forNext(
            /* resultSlotId */ null,
            lookahead,
            /* limitPerChannel */ limit
        );
        return reader.listNext(criteria, PageRequest.of(0, limit)).items();
    }
}
