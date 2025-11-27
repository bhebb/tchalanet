package com.tchalanet.server.ticket.application;

import com.tchalanet.server.ticket.domain.model.Ticket;
import com.tchalanet.server.ticket.domain.ports.in.GetTicketDetailsQuery;
import com.tchalanet.server.ticket.domain.ports.out.TicketRepositoryPort;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetTicketDetailsService implements GetTicketDetailsQuery {

  private final TicketRepositoryPort ticketRepository;

  // private final DrawReadModelPort drawReadModel; // To resolve draw info

  @Override
  public Optional<TicketDetailsDto> findByPublicCode(String publicCode) {
    return ticketRepository.findByPublicCode(publicCode).map(this::toDto);
  }

  @Override
  public Optional<TicketDetailsDto> findById(UUID id) {
    return ticketRepository.findById(id).map(this::toDto);
  }

  private TicketDetailsDto toDto(Ticket ticket) {
    // DrawInfo drawInfo = drawReadModel.findById(ticket.getDrawId()); // Placeholder
    DrawInfo drawInfo = new DrawInfo(ticket.getDrawId(), "Resolved Draw Name", Instant.now());

    var lines =
        ticket.getLines().stream()
            .map(
                line ->
                    new LineInfo(
                        line.gameCode(), line.selection(), line.stake(), line.potentialPayout()))
            .collect(Collectors.toList());

    return new TicketDetailsDto(
        ticket.getId(),
        ticket.getTenantId(),
        ticket.getTicketCode(),
        ticket.getPublicCode(),
        ticket.getStatus(),
        ticket.getTotalAmount(),
        ticket.getCreatedAt(),
        drawInfo,
        lines);
  }
}
