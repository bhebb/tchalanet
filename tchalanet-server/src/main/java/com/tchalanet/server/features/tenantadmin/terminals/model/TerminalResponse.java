package com.tchalanet.server.features.tenantadmin.terminals.model;

import com.tchalanet.server.core.pos.domain.model.Terminal;
import java.util.UUID;

public record TerminalResponse(UUID id, UUID outletId, String deviceId, String label, String status) {

  public static TerminalResponse fromDomain(Terminal t) {
    return new TerminalResponse(t.id().value(), t.outletId() == null ? null : t.outletId().value(), t.deviceId(), t.label(), t.status());
  }
}
