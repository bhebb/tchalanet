package com.tchalanet.server.core.session.internal.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.session.internal.application.port.out.SalesSessionWriterPort;
import com.tchalanet.server.core.session.internal.domain.model.SalesSession;
import com.tchalanet.server.core.session.internal.infra.persistence.SalesSessionJpaRepository;
import com.tchalanet.server.core.session.internal.infra.persistence.SalesSessionMapper;
import java.time.Instant;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SalesSessionWriterJpaAdapter implements SalesSessionWriterPort {

    private final SalesSessionJpaRepository repo;
    private final SalesSessionMapper mapper;

    @Override
    public SalesSession save(SalesSession session) {
        var existing = repo.findByTenantIdAndId(session.tenantId().value(), session.id().value());
        if (existing.isEmpty()) {
            var entity = mapper.toEntity(session);
            return mapper.toDomain(repo.save(entity));
        }

        var entity = existing.get();
        assertImmutableFields(entity, session);
        mapper.applyToEntity(session, entity);
        return mapper.toDomain(entity);
    }

    @Override
    public void finalizeSession(SalesSessionId sessionId, Instant finalizedAt, UserId finalizedBy, String reason) {
        var entity =
            repo.getReferenceById(sessionId.value());
        // TODO: implement finalization fields when SalesSession entity exposes them
        repo.save(entity);
    }

    private static void assertImmutableFields(
        com.tchalanet.server.core.session.internal.infra.persistence.SalesSessionJpaEntity entity,
        SalesSession session
    ) {
        requireSame("sessionId", entity.getId(), session.id().value());
        requireSame("tenantId", entity.getTenantId(), session.tenantId().value());
        requireSame("outletId", entity.getOutletId(), session.outletId().value());
        requireSame(
            "terminalId",
            entity.getTerminalId(),
            session.terminalId() == null ? null : session.terminalId().value());
        requireSame("openedBy", entity.getOpenedBy(), session.openedBy().value());
        requireSame("openedAt", entity.getOpenedAt(), session.openedAt());
        requireSame("businessDate", entity.getBusinessDate(), session.businessDate());
        requireSame("openingFloatCents", entity.getOpeningFloatCents(), session.openingFloatCents());
    }

    private static void requireSame(String field, Object actual, Object expected) {
        if (!Objects.equals(actual, expected)) {
            throw new IllegalStateException(
                "SalesSession immutable field changed: "
                    + field
                    + " expected="
                    + actual
                    + " actual="
                    + expected);
        }
    }
}
