package com.tchalanet.server.features.tenantadmin.terminals.model;

import com.tchalanet.server.core.terminal.domain.model.Terminal;
import java.util.UUID;

public record TerminalResponse(UUID id, UUID outletId, String label, String status) {

  public static TerminalResponse fromDomain(Terminal t) {
    return new TerminalResponse(t.id(), t.outletId() == null ? null : t.outletId().value(), t.label(), t.state() != null ? t.state().name() : null);
  }
}
