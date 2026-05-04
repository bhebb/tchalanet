package com.tchalanet.server.core.draw.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.port.out.DrawSummaryReaderPort;
import com.tchalanet.server.core.draw.application.query.model.GetDrawByIdQuery;
import com.tchalanet.server.core.draw.domain.model.DrawSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class GetDrawByIdQueryHandler implements QueryHandler<GetDrawByIdQuery, DrawSummary> {

    private final DrawSummaryReaderPort reader;

    @Override
    public DrawSummary handle(GetDrawByIdQuery query) {
        return reader.getById(query.id());
    }
}

