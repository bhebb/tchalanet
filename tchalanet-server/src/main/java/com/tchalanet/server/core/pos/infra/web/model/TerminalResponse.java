package com.tchalanet.server.core.pos.infra.web.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;
import java.util.UUID;

public record TerminalResponse(
    UUID id,
    TenantId tenantId,
    OutletId outletId,
    String state,
    Instant lastSeen,
    String meta,
    long version,
    Instant registeredAt,
    Instant unregisteredAt,
    Instant lockedAt,
    UUID lockedBy,
    String lockReason,
    Instant deletedAt,
    String label,
    String inventoryTag) {

  public static TerminalResponse fromDomain(com.tchalanet.server.core.pos.domain.model.Terminal t) {
    if (t == null) return null;
    return new TerminalResponse(
        t.id(),
        t.tenantId(),
        t.outletId(),
        t.state() == null ? null : t.state().name(),
        t.lastSeen(),
        t.meta(),
        t.version(),
        t.registeredAt(),
        t.unregisteredAt(),
        t.lockedAt(),
        t.lockedBy(),
        t.lockReason(),
        t.deletedAt(),
        t.label(),
        t.inventoryTag());
  }
}
