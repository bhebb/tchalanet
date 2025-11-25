package com.tchalanet.server.common.audit.domain.usecase;

public interface PurgeOldAuditEventsUseCase {
  /** Purge old audit events according to retention policy. */
  void purge();
}
