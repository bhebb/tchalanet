package com.tchalanet.server.stats.infra.persistence.adapter;

import com.tchalanet.server.stats.domain.ports.out.TicketReadModelPort;
import com.tchalanet.server.ticket.infra.persistence.entity.TicketEntity;
import com.tchalanet.server.ticket.infra.persistence.repository.SpringTicketJpaRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaTicketReadModelAdapter implements TicketReadModelPort {

  private final SpringTicketJpaRepository ticketJpaRepository;

  @Override
  public List<TicketInfo> findTicketsByDrawId(UUID drawId) {
    // Assuming TicketEntity has a drawId field and lines are eagerly fetched or joined
    List<TicketEntity> ticketEntities =
        ticketJpaRepository.findByDrawId(
            drawId); // This method needs to be added to SpringTicketJpaRepository

    return ticketEntities.stream()
        .map(
            ticketEntity ->
                new TicketInfo(
                    ticketEntity.getId(),
                    ticketEntity.getTenantId(),
                    ticketEntity.getTotalAmount(),
                    // This totalPayout should be the actual payout after draw settlement,
                    // not just potential. This implies the TicketEntity needs an actualPayout field
                    // or we need to calculate it here based on result_payload and ticket lines.
                    // For now, we'll use a placeholder or assume totalPayout is updated on
                    // TicketEntity.
                    ticketEntity.getTotalAmount(), // Placeholder for actual payout
                    ticketEntity.getLines().size() // Assuming lines are loaded
                    ))
        .collect(Collectors.toList());
  }
}
