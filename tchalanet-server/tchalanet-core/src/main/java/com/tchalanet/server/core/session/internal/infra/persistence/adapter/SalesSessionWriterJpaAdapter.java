package com.tchalanet.server.core.session.internal.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.session.internal.application.port.out.SalesSessionWriterPort;
import com.tchalanet.server.core.session.internal.domain.model.SalesSession;
import com.tchalanet.server.core.session.internal.infra.persistence.SalesSessionJpaRepository;
import com.tchalanet.server.core.session.internal.infra.persistence.SalesSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class SalesSessionWriterJpaAdapter implements SalesSessionWriterPort {

    private final SalesSessionJpaRepository repo;
    private final SalesSessionMapper mapper;

    @Override
    public SalesSession save(SalesSession session) {
        var entity = mapper.toEntity(session);
        var savedEntity = repo.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public void finalizeSession(SalesSessionId sessionId, Instant finalizedAt, UserId finalizedBy, String reason) {
        var entity =
            repo.getReferenceById(sessionId.value());
        // TODO: implement finalization fields when SalesSession entity exposes them
        repo.save(entity);
    }

}
