package com.tchalanet.server.core.session.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.session.application.port.out.PosSessionReaderPort;
import com.tchalanet.server.core.session.application.query.model.GetCurrentSessionQuery;
import com.tchalanet.server.core.session.domain.model.PosSession;

import java.util.Optional;

import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetCurrentSessionQueryHandler implements QueryHandler<GetCurrentSessionQuery, Optional<PosSession>> {

    private final PosSessionReaderPort posSessionRepository;

    @Override
    public Optional<PosSession> handle(GetCurrentSessionQuery query) {
        return posSessionRepository.findOpenByTerminal(query.tenantId(), query.terminalId());
    }
}
