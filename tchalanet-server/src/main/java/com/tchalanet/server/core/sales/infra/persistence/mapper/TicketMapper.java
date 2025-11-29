package com.tchalanet.server.core.sales.infra.persistence.mapper;

import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.core.sales.domain.model.TicketLine;
import com.tchalanet.server.core.sales.infra.persistence.entity.TicketEntity;
import com.tchalanet.server.core.sales.infra.persistence.entity.TicketLineEntity;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class TicketMapper {

  public TicketEntity toEntity(Ticket ticket) {
    TicketEntity entity = new TicketEntity();
    entity.setId(ticket.getId());
    entity.setTenantId(ticket.getTenantId());
    entity.setTerminalId(ticket.getTerminalId());
    entity.setSessionId(ticket.getSessionId());
    entity.setDrawId(ticket.getDrawId());
    entity.setTicketCode(ticket.getTicketCode());
    entity.setPublicCode(ticket.getPublicCode());
    entity.setStatus(ticket.getStatus());
    entity.setTotalAmount(ticket.getTotalAmount());
    entity.setCreatedAt(ticket.getCreatedAt());
    entity.setUpdatedAt(ticket.getUpdatedAt());

    entity.setLines(
        ticket.getLines().stream()
            .map(line -> toLineEntity(line, entity))
            .collect(Collectors.toList()));

    return entity;
  }

  public Ticket toDomain(TicketEntity entity) {
    // This is a simplified mapping. A more robust implementation would use reflection
    // or a private constructor on the domain object if it needs to be reconstructed
    // from a persisted state. For now, we assume we can create it via the factory.
    // This highlights a common challenge in DDD: reconstructing aggregates.
    // A dedicated constructor in Ticket for reconstruction is a good pattern.
    return Ticket.create(
        entity.getTenantId(),
        entity.getTerminalId(),
        entity.getSessionId(),
        entity.getDrawId(),
        entity.getTicketCode(),
        entity.getPublicCode(),
        entity.getLines().stream().map(this::toLineDomain).collect(Collectors.toList()));
  }

  private TicketLineEntity toLineEntity(TicketLine line, TicketEntity ticketEntity) {
    TicketLineEntity lineEntity = new TicketLineEntity();
    lineEntity.setTicket(ticketEntity);
    lineEntity.setGameCode(line.gameCode());
    lineEntity.setSelection(line.selection());
    lineEntity.setStake(line.stake());
    lineEntity.setOddsSnapshot(line.oddsSnapshot());
    lineEntity.setPotentialPayout(line.potentialPayout());
    return lineEntity;
  }

  private TicketLine toLineDomain(TicketLineEntity lineEntity) {
    return new TicketLine(
        lineEntity.getGameCode(),
        lineEntity.getSelection(),
        lineEntity.getStake(),
        lineEntity.getOddsSnapshot(),
        lineEntity.getPotentialPayout());
  }
}
