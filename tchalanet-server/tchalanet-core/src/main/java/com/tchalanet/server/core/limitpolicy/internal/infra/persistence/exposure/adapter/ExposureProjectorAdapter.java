package com.tchalanet.server.core.limitpolicy.internal.infra.persistence.exposure.adapter;

import com.tchalanet.server.core.selection.api.SelectionApi;
import com.tchalanet.server.core.limitpolicy.internal.application.port.out.exposure.ExposureProjectorPort;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.LimitScopeRef;
import com.tchalanet.server.core.limitpolicy.internal.infra.persistence.exposure.ScopePersistenceMapper;
import com.tchalanet.server.core.sales.api.event.TicketPlacedEvent;
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

    private final SelectionApi selectionApi;
    @Override
    public void applyTicketSold(TicketPlacedEvent event) {
        var scopes = scopesFor(event);

        for (var scope : scopes) {
            var scopeRow = ScopePersistenceMapper.toRow(scope);

            for (var line : event.lines()) {
                var stake = line.stakeAmount().amount();
                var payout = line.potentialPayoutAmount().amount();

                var canonicalSelection =
                    selectionApi.canonicalize(
                        line.betType(),
                        line.selectionKey());

                jdbc.update("""
            SELECT increment_draw_exposure(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
                    event.tenantId().value(),
                    event.context().drawId().value(),
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

        if (event.context().drawChannelId() != null) {
            scopes.add(LimitScopeRef.drawChannel(event.context().drawChannelId()));
        }

        if (event.context().outletId() != null) {
            scopes.add(LimitScopeRef.outlet(event.context().outletId()));
        }

        if (event.context().sellerUserId() != null) {
            scopes.add(LimitScopeRef.agent(event.context().sellerUserId()));
        }

        return List.copyOf(scopes);
    }
}
