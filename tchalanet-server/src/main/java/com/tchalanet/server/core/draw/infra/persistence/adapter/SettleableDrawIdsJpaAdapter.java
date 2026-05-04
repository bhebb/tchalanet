package com.tchalanet.server.core.draw.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.draw.application.port.out.FindSettleableDrawIdsPort;
import com.tchalanet.server.core.draw.infra.persistence.repo.DrawBatchQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class SettleableDrawIdsJpaAdapter implements FindSettleableDrawIdsPort {

    private final DrawBatchQueryRepository repo;

    @Override
    public List<DrawId> findSettleableDrawIds(SettleableDrawCriteria criteria) {
        Objects.requireNonNull(criteria, "criteria is required");
        Objects.requireNonNull(criteria.tenantId(), "tenantId is required");
        Objects.requireNonNull(criteria.from(), "from is required");
        Objects.requireNonNull(criteria.to(), "to is required");

        return repo.findSettleableDrawIds(
                criteria.tenantId().value(),
                Timestamp.from(criteria.from()),
                Timestamp.from(criteria.to()),
                criteria.maxDraws(),
                criteria.force()
            )
            .stream()
            .map(DrawId::nullableOf)
            .toList();
    }
}
