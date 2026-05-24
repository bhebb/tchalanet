package com.tchalanet.server.core.draw.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawSummaryReaderPort;
import com.tchalanet.server.core.draw.api.query.GetDrawByIdQuery;
import com.tchalanet.server.core.draw.api.query.DrawSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class GetDrawByIdQueryHandler implements QueryHandler<GetDrawByIdQuery, DrawSummary> {

    private final DrawSummaryReaderPort reader;

    @Override
    public DrawSummary handle(GetDrawByIdQuery query) {
        Objects.requireNonNull(query, "query is required");
        Objects.requireNonNull(query.id(), "id is required");

        return reader.getById(query.id());
    }
}

