package com.tchalanet.server.pos.infra.persistence;

import com.tchalanet.server.pos.domain.model.TicketLine;
import com.tchalanet.server.pos.domain.model.TicketLineId;
import com.tchalanet.server.pos.domain.ports.TicketLineRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaTicketLineRepositoryAdapter implements TicketLineRepository {

  private final TicketLineJpaRepository jpa;

  @Override
  public Optional<TicketLine> findById(TicketLineId id) {
    if (id == null) return Optional.empty();
    return jpa.findById(id.value())
        .map(
            e ->
                new TicketLine(
                    TicketLineId.of(e.getId()),
                    e.getTicket() == null ? null : e.getTicket().getId(),
                    e.getGameCode(),
                    e.getSelection(),
                    e.getStake(),
                    e.getOddsSnapshot(),
                    e.getPotentialPayout()));
  }

  @Override
  public TicketLine save(TicketLine l) {
    var e = new TicketLineJpaEntity();
    if (l.id() != null) e.setId(l.id().value());
    else e.setId(UUID.randomUUID());
    // mapping ticket relation is omitted for brevity; callers should set relationships via adapters
    e.setGameCode(l.gameCode());
    e.setSelection(l.selection());
    e.setStake(l.stake());
    e.setOddsSnapshot(l.oddsSnapshot());
    e.setPotentialPayout(l.potentialPayout());
    var saved = jpa.save(e);
    return new TicketLine(
        TicketLineId.of(saved.getId()),
        saved.getTicket() == null ? null : saved.getTicket().getId(),
        saved.getGameCode(),
        saved.getSelection(),
        saved.getStake(),
        saved.getOddsSnapshot(),
        saved.getPotentialPayout());
  }
}
