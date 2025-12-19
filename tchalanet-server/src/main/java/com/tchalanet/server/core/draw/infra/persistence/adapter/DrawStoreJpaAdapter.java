package com.tchalanet.server.core.draw.infra.persistence.adapter;

import com.tchalanet.server.core.draw.application.port.out.DrawStorePort;
import com.tchalanet.server.core.draw.application.query.projection.DueToCloseRow;
import com.tchalanet.server.core.draw.application.query.projection.NewDrawRow;
import com.tchalanet.server.core.draw.infra.persistence.repo.DrawJpaRepositoryV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DrawStoreJpaAdapter implements DrawStorePort {

    private final DrawJpaRepositoryV1 repo;

    @Override
    public boolean exists(UUID tenantId, UUID drawChannelId, Instant scheduledAt) {
        return repo.existsByTenantIdAndDrawChannelIdAndScheduledAtAndDeletedAtIsNull(
            tenantId, drawChannelId, scheduledAt);
    }

    @Override
    public int bulkInsert(List<NewDrawRow> rows) {
        int created = 0;
        for (NewDrawRow r : rows) {
            try {
                created +=
                    repo.insertIfNotExists(
                        r.tenantId(),
                        r.drawChannelId(),
                        r.scheduledAt(),
                        r.cutoffSec(),
                        r.status(),
                        r.drawSource(),
                        r.systemGenerated(),
                        r.locked());
            } catch (DataIntegrityViolationException e) {
                log.warn(e.getMessage(), e);
            }
        }
        return created;
    }

    @Override
    public List<DueToCloseRow> findDueToClose(Instant now, int limit) {
        return repo.findDueToClose(now, limit).stream()
            .map(p -> new DueToCloseRow(p.getTenantId(), p.getDrawId(), Boolean.TRUE.equals(p.getLocked())))
            .toList();
    }

    @Override
    public int bulkClose(List<UUID> drawIds) {
        if (drawIds == null || drawIds.isEmpty()) return 0;
        UUID[] ids = drawIds.toArray(new UUID[0]);
        return repo.bulkClose(ids);
    }
}
