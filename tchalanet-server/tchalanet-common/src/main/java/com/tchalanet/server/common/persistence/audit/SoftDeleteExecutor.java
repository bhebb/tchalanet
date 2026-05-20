package com.tchalanet.server.common.persistence.audit;

import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.persistence.AuditableEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class SoftDeleteExecutor {

    private final Clock clock;

    public <T extends AuditableEntity> T softDelete(T entity) {
        var ctx = TchContext.currentOrNull();
        if (ctx == null) {
            return entity;
        }
        var userId = ctx.userUuid();

        entity.softDelete(userId, Instant.now(clock));
        return entity;
    }

    public <T extends AuditableEntity> T restore(T entity) {
        entity.restore();
        return entity;
    }
}
