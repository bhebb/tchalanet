package com.tchalanet.server.core.sales.infra.persistence;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.application.port.out.TicketWriterPort;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TicketJpaAdapter implements TicketReaderPort, TicketWriterPort {

  private final TicketJpaRepository ticketRepo;
  private final TicketLineJpaRepository lineRepo;
  private final TicketJpaMapper mapper;

  @Override
  public Ticket save(Ticket ticket) {
    var saved = ticketRepo.save(mapper.toEntity(ticket));
    lineRepo.deleteAll(lineRepo.findByTicketIdOrderByLineNo(saved.getId()));
    lineRepo.saveAll(ticket.lines().stream().map(l -> mapper.toLineEntity(ticket, l)).toList());
    return mapper.toDomain(saved, lineRepo.findByTicketIdOrderByLineNo(saved.getId()));
  }

  @Override
  public Optional<Ticket> findById(TicketId ticketId) {
    return ticketRepo.findByIdAndDeletedAtIsNull(ticketId.value())
        .map(e -> mapper.toDomain(e, lineRepo.findByTicketIdOrderByLineNo(e.getId())));
  }

  @Override
  public boolean existsAcceptedOfflineCode(String offlineCode) {
    // TODO use current tenant from RLS context or add tenant parameter to port.
    return false;
  }

  @Override
  public boolean existsAcceptedLocalSequence(String terminalKey, long localSequence) {
    // TODO implement query.
    return false;
  }
}
