package com.tchalanet.server.core.sales.infra.persistence.mapper;

import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.core.sales.domain.model.TicketLine;
import com.tchalanet.server.core.sales.infra.persistence.TicketEntity;
import com.tchalanet.server.core.sales.infra.persistence.TicketLineEntity;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class TicketMapper {

    public Ticket toDomain(TicketEntity e) {
        var lines = e.getLines() == null ? List.<TicketLine>of()
            : e.getLines().stream().map(this::toDomainLine).toList();

        // Reconstruct: keep exact persisted fields
        return Ticket.rehydrate(
            e.getId(),
            e.getTenantId(),
            e.getTerminalId(),
            e.getSessionId(),
            e.getDrawId(),
            e.getTicketCode(),
            e.getPublicCode(),
            lines,
            e.getTotalAmount(),
            e.getStatus(),
            e.getCreatedAt(),
            e.getUpdatedAt()
        );
    }

    private TicketLine toDomainLine(TicketLineEntity le) {
        return new TicketLine(
            le.getGameCode(),
            le.getSelection(),
            le.getStake(),
            le.getOddsSnapshot(),
            le.getPotentialPayout(),
            le.getBetType()
        );
    }

    public TicketEntity toEntity(Ticket domain) {
        TicketEntity e = new TicketEntity();
        e.setId(domain.getId());            // BaseTenantEntity
        e.setTenantId(domain.getTenantId());

        e.setTerminalId(domain.getTerminalId());
        e.setSessionId(domain.getSessionId());
        e.setDrawId(domain.getDrawId());

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
