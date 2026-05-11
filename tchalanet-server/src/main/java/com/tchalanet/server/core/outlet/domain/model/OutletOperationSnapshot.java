package com.tchalanet.server.core.outlet.domain.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;

public record OutletOperationSnapshot(
    TenantId tenantId,
    OutletId outletId,
    String name,
    String slug,
    OutletStatus status,
    boolean dayClosed,
    boolean salesBlocked,
    String salesBlockReason,
    Instant salesBlockedAt,
    boolean payoutBlocked,
    String payoutBlockReason,
    Instant payoutBlockedAt,
    UserId payoutBlockedBy,
    boolean offlineSalesBlocked,
    String offlineSalesBlockReason,
    Instant offlineSalesBlockedAt,
    UserId offlineSalesBlockedBy,
    String timezone
) {
  public boolean active() {
    return status == OutletStatus.ACTIVE;
  }

  public boolean salesAllowed() {
    return active() && !dayClosed && !salesBlocked;
  }

  public boolean payoutAllowed() {
    return active() && !dayClosed && !payoutBlocked;
  }

  public boolean offlineSalesAllowedForGrant() {
    return active() && !dayClosed && !salesBlocked && !offlineSalesBlocked;
  }

  public boolean canReceiveOfflineSyncForAudit() {
    return status != OutletStatus.ARCHIVED;
  }
}
