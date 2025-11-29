package com.tchalanet.server.core.pos.infra.persistence;

import com.tchalanet.server.core.pos.domain.model.Ticket;
import com.tchalanet.server.core.pos.domain.ports.TicketRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaTicketRepositoryAdapter implements TicketRepository {

  private final TicketJpaRepository jpa;

  @Override
  public Optional<Ticket> findById(UUID id) {
    return jpa.findById(id)
        .map(
            e ->
                new Ticket(
                    e.getId(),
                    e.getTenantId(),
                    e.getPublicCode(),
                    e.getTerminal() == null ? null : e.getTerminal().getId(),
                    e.getDraw() == null ? null : e.getDraw().getId(),
                    e.getStatus(),
                    e.getTotalAmount(),
                    e.getCreatedAt()));
  }

  @Override
  public Ticket save(Ticket t) {
    var e = new TicketJpaEntity();
    e.setId(t.id());
    e.setTenantId(t.tenantId());
    e.setPublicCode(t.publicCode());
    e.setStatus(t.status());
    e.setTotalAmount(t.totalAmount());
    var saved = jpa.save(e);
    return new Ticket(
        saved.getId(),
        saved.getTenantId(),
        saved.getPublicCode(),
        saved.getTerminal() == null ? null : saved.getTerminal().getId(),
        saved.getDraw() == null ? null : saved.getDraw().getId(),
        saved.getStatus(),
        saved.getTotalAmount(),
        saved.getCreatedAt());
  }
}
