package com.tchalanet.server.core.offlinesync.domain.service;

import com.tchalanet.server.core.offlinesync.domain.model.OfflineSalesGrant;
import com.tchalanet.server.core.offlinesync.domain.model.OfflineSalesGrantStatus;
import java.time.Instant;

public class OfflineGrantPolicy {

  public boolean isUsable(OfflineSalesGrant grant, Instant at) {
    return grant.status() == OfflineSalesGrantStatus.ACTIVE && !grant.expiresAt().isBefore(at);
  }

  public boolean canBeRevoked(OfflineSalesGrant grant) {
    return grant.status() == OfflineSalesGrantStatus.ACTIVE;
  }
}

