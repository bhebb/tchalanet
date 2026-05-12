package com.tchalanet.server.core.limitpolicy.internal.infra.persistence.exposure.adapter;

import com.tchalanet.server.common.selection.SelectionKeyCanonicalizer;
import com.tchalanet.server.core.limitpolicy.internal.application.port.out.exposure.ExposureProjectorPort;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.LimitScopeRef;
import com.tchalanet.server.core.limitpolicy.internal.infra.persistence.exposure.ScopePersistenceMapper;
import com.tchalanet.server.core.sales.internal.domain.event.TicketPlacedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ExposureProjectorAdapter implements ExposureProjectorPort {

    private final JdbcTemplate jdbc;

    @Override
    public void applyTicketSold(TicketPlacedEvent event) {
        var scopes = scopesFor(event);

        for (var scope : scopes) {
            var scopeRow = ScopePersistenceMapper.toRow(scope);

            for (var line : event.lines()) {
                var stake = BigDecimal.valueOf(line.stakeCents(), 2);
                var payout = BigDecimal.valueOf(line.potentialPayoutCents(), 2);

                var canonicalSelection =
                    SelectionKeyCanonicalizer.canonicalize(
                        line.betType(),
                        line.selectionKeyRaw());

                jdbc.update("""
            SELECT increment_draw_exposure(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
                    event.tenantId().value(),
                    event.drawId().value(),
                    scopeRow.scopeType().name(),
                    scopeRow.scopeId(),
                    line.betType().name(),
                    canonicalSelection,
                    stake,
                    payout,
                    event.eventId().value(),
                    Timestamp.from(event.occurredAt())
                );
            }
        }
    }

    private List<LimitScopeRef> scopesFor(TicketPlacedEvent event) {
        var scopes = new ArrayList<LimitScopeRef>();

        scopes.add(LimitScopeRef.tenant(event.tenantId()));

        if (event.drawChannelId() != null) {
            scopes.add(LimitScopeRef.drawChannel(event.drawChannelId()));
        }

        if (event.outletId() != null) {
            scopes.add(LimitScopeRef.outlet(event.outletId()));
        }

        if (event.agentId() != null) {
            scopes.add(LimitScopeRef.agent(event.agentId()));
        }

        return List.copyOf(scopes);
    }
}
