package com.tchalanet.server.core.sales.infra.persistence.mapper;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.SessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.core.sales.domain.model.TicketLine;
import com.tchalanet.server.core.sales.infra.persistence.TicketEntity;
import com.tchalanet.server.core.sales.infra.persistence.TicketLineEntity;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TicketMapper {

  public Ticket toDomain(TicketEntity e) {
    var lines =
        e.getLines() == null
            ? List.<TicketLine>of()
            : e.getLines().stream().map(this::toDomainLine).toList();

    // Reconstruct: keep exact persisted fields
    return Ticket.rehydrate(
        TicketId.of(e.getId()),
        TenantId.of(e.getTenantId()),
        TerminalId.of(e.getTerminalId()),
        e.getSessionId() == null ? null : SessionId.of(e.getSessionId()),
        DrawId.of(e.getDrawId()),
        e.getTicketCode(),
        e.getPublicCode(),
        lines,
        e.getTotalAmount(),
        e.getStatus(),
        e.getCreatedAt(),
        e.getUpdatedAt());
  }

  private TicketLine toDomainLine(TicketLineEntity le) {
    return new TicketLine(
        le.getGameCode(),
        le.getSelection(),
        le.getStake(),
        le.getOddsSnapshot(),
        le.getPotentialPayout(),
        le.getBetType());
  }

  public TicketEntity toEntity(Ticket domain) {
    TicketEntity e = new TicketEntity();
    e.setId(domain.getId().uuid()); // BaseTenantEntity
    e.setTenantId(domain.getTenantId().uuid());

    e.setTerminalId(domain.getTerminalId().uuid());
    e.setSessionId(domain.getSessionId() == null ? null : domain.getSessionId().uuid());
    e.setDrawId(domain.getDrawId().uuid());

    e.setTicketCode(domain.getTicketCode());
    e.setPublicCode(domain.getPublicCode());
    e.setStatus(domain.getStatus());
    e.setTotalAmount(domain.getTotalAmount());

    var lineEntities = domain.getLines().stream().map(this::toEntityLine).toList();
    e.clearAndAddLines(lineEntities);
    return e;
  }

  private TicketLineEntity toEntityLine(TicketLine line) {
    TicketLineEntity le = new TicketLineEntity();
    le.setGameCode(line.gameCode());
    le.setSelection(line.selection());
    le.setStake(line.stake());
    le.setOddsSnapshot(line.oddsSnapshot());
    le.setPotentialPayout(line.potentialPayout());
    le.setBetType(line.betType());
    return le;
  }
}
