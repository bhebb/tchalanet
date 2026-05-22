package com.tchalanet.server.core.offlinesync.internal.infra.persistence.adapter;


import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineEventOutboxPort;
import com.tchalanet.server.core.offlinesync.internal.infra.persistence.OfflineEventOutboxJpaEntity;
import com.tchalanet.server.core.offlinesync.internal.infra.persistence.OfflineEventOutboxJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * JPA-backed {@link OfflineEventOutboxPort}. Serializes the event with Jackson and stores
 * its FQN so the drainer can deserialize and republish later.
 */
@Component
@RequiredArgsConstructor
public class OfflineEventOutboxJpaAdapter implements OfflineEventOutboxPort {

    private final OfflineEventOutboxJpaRepository repo;
    private final JsonUtils jsonUtils;
    private final Clock clock;

    @Override
    public void record(DomainEvent event) {
        var entity = new OfflineEventOutboxJpaEntity();
        entity.setTenantId(event.tenantId().value());
        entity.setEventId(event.eventId().value());
        entity.setEventClass(event.getClass().getName());
        entity.setPayloadJson(jsonUtils.toJson(event));
        entity.setCreatedAt(clock.instant());
        entity.setAttempts(0);
        repo.save(entity);
    }

}
