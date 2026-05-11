package com.tchalanet.server.core.limitpolicy.infra.persistence.exposure.adapter;

import com.tchalanet.server.core.limitpolicy.application.port.out.exposure.ExposureFactsReaderPort;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitContext;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitFactsSnapshot;
import com.tchalanet.server.core.limitpolicy.infra.persistence.exposure.DrawExposureJpaRepository;
import com.tchalanet.server.core.limitpolicy.infra.persistence.exposure.ScopePersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ExposureFactsReaderAdapter implements ExposureFactsReaderPort {

    private final DrawExposureJpaRepository repo;

    @Override
    public LimitFactsSnapshot snapshot(LimitContext ctx) {
        var betTypes = ctx.lines().stream()
            .map(line -> line.betType())
            .distinct()
            .toList();

        if (ctx.drawId() == null || betTypes.isEmpty()) {
            return new LimitFactsSnapshot(Map.of());
        }

        var facts = new HashMap<LimitFactsSnapshot.Key, LimitFactsSnapshot.Fact>();

        for (var scope : ctx.scopes()) {
            var scopeRow = ScopePersistenceMapper.toRow(scope);

            var rows = repo.findFactsForBetTypes(
                ctx.drawId().value(),
                scopeRow.scopeType(),
                scopeRow.scopeId(),
                betTypes);

            for (var entity : rows) {
                var key = new LimitFactsSnapshot.Key(
                    scope,
                    entity.getBetType(),
                    entity.getSelectionKey());

                facts.put(key, new LimitFactsSnapshot.Fact(
                    cents(entity.getStakeTotal()),
                    cents(entity.getPotentialPayoutTotal()),
                    entity.getSalesCount()));
            }
        }

        return new LimitFactsSnapshot(facts);
    }

    private static long cents(BigDecimal value) {
        if (value == null) {
            return 0L;
        }

        return value.movePointRight(2).longValueExact();
    }
}
