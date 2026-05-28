package com.tchalanet.server.core.drawresult.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.drawresult.internal.application.exception.DrawResultNotFoundException;
import com.tchalanet.server.core.drawresult.internal.application.port.out.DrawResultReaderPort;
import com.tchalanet.server.core.drawresult.api.query.GetDrawResultViewByIdQuery;
import com.tchalanet.server.core.drawresult.api.query.view.DrawResultView;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetDrawResultViewByIdQueryHandler
    implements QueryHandler<GetDrawResultViewByIdQuery, DrawResultView> {

    private final DrawResultReaderPort reader;

    @Override
    public DrawResultView handle(GetDrawResultViewByIdQuery query) {
        return reader.findViewById(query.id())
            .orElseThrow(() -> new DrawResultNotFoundException("DrawResult not found with id=" + query.id()));
    }
}
