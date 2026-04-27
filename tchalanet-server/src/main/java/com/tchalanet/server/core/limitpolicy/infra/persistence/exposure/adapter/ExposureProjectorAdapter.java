package com.tchalanet.server.core.limitpolicy.infra.persistence.exposure.adapter;

import com.tchalanet.server.core.limitpolicy.application.port.out.ExposureProjectorPort;
import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.core.limitpolicy.infra.persistence.exposure.mapper.ScopePersistenceMapper;
import com.tchalanet.server.core.sales.domain.event.TicketPlacedEvent;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitScopeRef;
import com.tchalanet.server.common.selection.SelectionKeyCanonicalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class ExposureProjectorAdapter implements ExposureProjectorPort {

  private final JdbcTemplate jdbc;
  private final TchContextResolver ctxResolver;

  @Override
  public void applyTicketPlaced(TicketPlacedEvent event) {
    var req = ctxResolver.currentOrThrow();
    var tenantId = req.tenantId().value();
    var actorId = req.userUuid(); // may be null

    // derive LimitScopeRef from event: prefer terminal > agent > outlet > drawChannel > tenant
    LimitScopeRef scope = switch (event) {
      case TicketPlacedEvent t when t.terminalId() != null -> new LimitScopeRef.TerminalScope(t.terminalId());
      case TicketPlacedEvent t when t.agentId() != null -> new LimitScopeRef.AgentScope(t.agentId());
      case TicketPlacedEvent t when t.outletId() != null -> new LimitScopeRef.OutletScope(t.outletId());
      case TicketPlacedEvent t when t.drawChannelId() != null -> new LimitScopeRef.DrawChannelScope(t.drawChannelId());
      default -> new LimitScopeRef.TenantScope(event.tenantId());
    };

    var scopeRow = ScopePersistenceMapper.toRow(scope, req.tenantId());

    for (var line : event.lines()) {
      var stake = BigDecimal.valueOf(line.stakeCents(), 2);
      var payout = BigDecimal.valueOf(line.potentialPayoutCents(), 2);

      // canonicalize selection key before persisting
      String canonicalSelection = SelectionKeyCanonicalizer.canonicalize(line.betType(), line.selectionKeyRaw());

      jdbc.update("""
          SELECT increment_draw_exposure(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
          """,
          tenantId,
          event.drawId().value(),
          scopeRow.scopeType().name(),
          scopeRow.scopeId(),
          line.betType().name(),
          canonicalSelection,
          stake,
          1L,
          payout,
          event.eventId().value(),                 // uuid
          java.sql.Timestamp.from(event.occurredAt()),
          actorId
      );
    }
  }
}
