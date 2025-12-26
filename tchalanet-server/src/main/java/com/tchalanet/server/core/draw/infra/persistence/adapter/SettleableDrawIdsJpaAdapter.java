package com.tchalanet.server.core.draw.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.draw.application.port.out.FindSettleableDrawIdsPort;
import com.tchalanet.server.core.draw.infra.persistence.repo.DrawBatchQueryRepository;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SettleableDrawIdsJpaAdapter implements FindSettleableDrawIdsPort {

    private final DrawBatchQueryRepository repo;

    @Override
    public List<DrawId> findSettleableDrawIds(FindSettleableDrawIdsPort.SettleableDrawCriteria criteria) {
        return repo.findSettleableDrawIds(
                criteria.tenantId().uuid(),
                criteria.source(),
                criteria.provider(),
                criteria.channelCode(),
                criteria.from(),
                criteria.to(),
                criteria.maxDraws(),
                criteria.force())
            .stream()
            .map(DrawId::nullableOf)
            .toList();
    }
}
