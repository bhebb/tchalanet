package com.tchalanet.server.core.drawresult.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.drawresult.application.port.out.DrawResultReaderPort;
import com.tchalanet.server.core.drawresult.application.port.out.DrawResultsCriteria;
import com.tchalanet.server.core.drawresult.application.query.model.ListDrawResultsQuery;
import com.tchalanet.server.core.drawresult.application.view.DrawResultView;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListDrawResultsQueryHandler
    implements QueryHandler<ListDrawResultsQuery, TchPage<DrawResultView>> {

    private final DrawResultReaderPort reader;

    @Override
    public TchPage<DrawResultView> handle(ListDrawResultsQuery query) {
        return reader.findViewsByCriteria(new DrawResultsCriteria(
            query.slotKey(),
            query.status(),
            query.quality(),
            query.from(),
            query.to(),
            query.pageable()
        ));
    }
}
