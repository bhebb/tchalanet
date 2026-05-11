package com.tchalanet.server.core.session.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.session.application.port.out.SalesSessionWriterPort;
import com.tchalanet.server.core.session.domain.model.SalesSession;
import com.tchalanet.server.core.session.domain.model.SalesSessionStatus;
import com.tchalanet.server.core.session.infra.persistence.SalesSessionJpaRepository;
import com.tchalanet.server.core.session.infra.persistence.SalesSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class SalesSessionWriterJpaAdapter implements SalesSessionWriterPort {

    private final SalesSessionJpaRepository repo;
    private final SalesSessionMapper mapper;
    private final SalesSessionWriterPort writer;

    @Override
    public long closeAllOpenSessions(OutletId outletId, UserId userId, long closingFloatCents, String reason) {
        var entities =
            repo.findByOutletIdAndStatus(
                outletId.value(), SalesSessionStatus.OPEN);
        long count = 0;
        for (var e : entities) {
            SalesSession session = mapper.toDomain(e);
            SalesSession updated = session.close(userId, Instant.now(), closingFloatCents, reason);
            writer.save(updated);
            count++;
        }
        return count;
    }

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
        entity.seF
    }

}
