package com.tchalanet.server.core.draw.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.error.ProblemRest;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.port.out.DrawLookupPort;
import com.tchalanet.server.core.draw.application.query.model.GetDrawByIdQuery;
import com.tchalanet.server.core.draw.domain.model.Draw;
import com.tchalanet.server.core.draw.domain.model.DrawSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class GetDrawByIdQueryHandler implements QueryHandler<GetDrawByIdQuery, DrawSummary> {

    private final DrawLookupPort drawLookupPort;

    @Override
    public DrawSummary handle(GetDrawByIdQuery query) {
        Draw draw = drawLookupPort.findById(query.id())
            .orElseThrow(() -> ProblemRest.notFound("Draw not found", query.id()));

        return new DrawSummary(
            draw.id(),
            draw.drawChannel().code(),
            draw.drawChannel().name(),
            draw.scheduledAt(),
            draw.cutoffAt(),
            draw.status(),
            false,
            draw.status() != com.tchalanet.server.core.draw.domain.model.DrawStatus.CANCELED,
            List.of());
    }
}

