package com.tchalanet.server.core.draw.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.port.out.DrawLookupPort;
import com.tchalanet.server.core.draw.application.query.model.GetDrawQuery;
import com.tchalanet.server.core.draw.domain.model.Draw;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class GetDrawHandler implements QueryHandler<GetDrawQuery, Draw> {

    private final DrawLookupPort drawReaderPort;

    @Override
    public Draw handle(GetDrawQuery query) {
        return drawReaderPort
            .findById(query.drawId())
            .orElseThrow(() -> new IllegalArgumentException("Draw not found: " + query.drawId()));
    }
}
