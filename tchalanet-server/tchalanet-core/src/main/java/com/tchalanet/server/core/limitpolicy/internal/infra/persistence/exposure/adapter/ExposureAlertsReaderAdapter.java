package com.tchalanet.server.core.limitpolicy.internal.infra.persistence.exposure.adapter;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.limitpolicy.internal.application.port.out.exposure.ExposureAlertsReaderPort;
import com.tchalanet.server.core.limitpolicy.api.model.LimitScopeRef;
import com.tchalanet.server.core.limitpolicy.internal.infra.persistence.exposure.DrawExposureJpaRepository;
import com.tchalanet.server.core.limitpolicy.internal.infra.persistence.exposure.ScopePersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ExposureAlertsReaderAdapter implements ExposureAlertsReaderPort {

    private final DrawExposureJpaRepository repo;

    @Override
    public List<Row> topByStake(
        DrawId drawId,
        LimitScopeRef scope,
        int limit
    ) {
        var scopeRow = ScopePersistenceMapper.toRow(scope);

        var rows = repo.topByStake(
            drawId.value(),
            scopeRow.scopeType(),
            scopeRow.scopeId(),
            PageRequest.of(0, normalizedLimit(limit)));

        return rows.stream()
            .map(this::toRow)
            .toList();
    }

    @Override
    public List<Row> topByPayout(
        DrawId drawId,
        LimitScopeRef scope,
        int limit
    ) {
        var scopeRow = ScopePersistenceMapper.toRow(scope);

        var rows = repo.topByPayout(
            drawId.value(),
            scopeRow.scopeType(),
            scopeRow.scopeId(),
            PageRequest.of(0, normalizedLimit(limit)));

        return rows.stream()
            .map(this::toRow)
            .toList();
    }

    private Row toRow(
        com.tchalanet.server.core.limitpolicy.internal.infra.persistence.exposure.DrawExposureJpaEntity entity
    ) {
        return new Row(
            entity.getBetType(),
            entity.getSelectionKey(),
            entity.getStakeTotal(),
            entity.getPotentialPayoutTotal(),
            entity.getSalesCount());
    }

    private int normalizedLimit(int limit) {
        return Math.min(Math.max(1, limit), 100);
    }
}
