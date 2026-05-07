package com.tchalanet.server.core.terminal.infra.integration;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.outlet.application.port.out.OutletTerminalReaderPort;
import com.tchalanet.server.core.outlet.application.query.model.OutletTerminalView;
import com.tchalanet.server.core.terminal.infra.persistence.TerminalJpaEntity;
import com.tchalanet.server.core.terminal.infra.persistence.TerminalJpaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Cross-domain adapter implementing {@link OutletTerminalReaderPort}.
 *
 * <p>Lives in {@code core.terminal} since it owns the {@code terminal.outlet_id} column. RLS
 * enforces tenant scoping at DB level.
 */
@Component
@RequiredArgsConstructor
public class OutletTerminalsAdapter implements OutletTerminalReaderPort {

  private final TerminalJpaRepository repo;

  @Override
  public List<OutletTerminalView> listTerminalsByOutlet(OutletId outletId) {
    // RLS-scoped fetch — listAll then filter by outletId in memory keeps the adapter simple.
    return repo.findAll().stream()
        .filter(t -> t.getDeletedAt() == null)
        .filter(t -> outletId.value().equals(t.getOutletId()))
        .map(this::toView)
        .toList();
  }

  @Override
  public long countTerminalsByOutlet(OutletId outletId) {
    return repo.findAll().stream()
        .filter(t -> t.getDeletedAt() == null)
        .filter(t -> outletId.value().equals(t.getOutletId()))
        .count();
  }

  private OutletTerminalView toView(TerminalJpaEntity t) {
    return new OutletTerminalView(
        TerminalId.of(t.getId()),
        OutletId.of(t.getOutletId()),
        t.getLabel(),
        t.getKind(),
        t.getState(),
        t.getSyncState(),
        UserId.nullableOf(t.getAssignedUserId()),
        t.isActiveForUser(),
        t.getLastSeen());
  }
}
