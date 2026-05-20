package com.tchalanet.server.core.draw.internal.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.draw.internal.application.port.out.FindSettleableDrawIdsPort;
import com.tchalanet.server.core.draw.internal.infra.persistence.repo.DrawBatchQueryRepository;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
                criteria.force())
            .stream()
            .map(DrawId::of)
            .toList();
    }
}
