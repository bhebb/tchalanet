package com.tchalanet.server.core.pos.infra.persistence;

import com.tchalanet.server.core.pos.domain.model.Terminal;
import com.tchalanet.server.core.pos.domain.ports.TerminalRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaTerminalRepositoryAdapter implements TerminalRepository {

  private final TerminalJpaRepository jpa;

  @Override
  public Optional<Terminal> findById(UUID id) {
    return jpa.findById(id)
        .map(
            e ->
                new Terminal(
                    e.getId(),
                    e.getTenantId(),
                    e.getOutlet() == null ? null : e.getOutlet().getId(),
                    e.getState(),
                    e.getLastSeen()));
  }

  @Override
  public Terminal save(Terminal t) {
    var e = new TerminalJpaEntity();
    e.setId(t.id());
    e.setTenantId(t.tenantId());
    // outlet relation left null for minimal adapter; caller should set via other adapter
    e.setState(t.state());
    e.setLastSeen(t.lastSeen());
    var saved = jpa.save(e);
    return new Terminal(
        saved.getId(),
        saved.getTenantId(),
        saved.getOutlet() == null ? null : saved.getOutlet().getId(),
        saved.getState(),
        saved.getLastSeen());
  }
}
