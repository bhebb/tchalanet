package com.tchalanet.server.core.draw.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.port.out.DrawChannelReaderPort;
import com.tchalanet.server.core.draw.application.query.model.GetDrawChannelQuery;
import com.tchalanet.server.core.draw.domain.model.DrawChannel;
import com.tchalanet.server.core.draw.domain.model.DrawChannelId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class GetDrawChannelHandler implements QueryHandler<GetDrawChannelQuery, DrawChannel> {

    private final DrawChannelReaderPort drawChannelReaderPort;

    @Override
    public DrawChannel handle(GetDrawChannelQuery query) {
        return drawChannelReaderPort
            .findById(query.tenantId(), query.id())
            .orElseThrow(
                () -> new IllegalArgumentException("DrawChannel not found: " + query.id()));
    }
}
