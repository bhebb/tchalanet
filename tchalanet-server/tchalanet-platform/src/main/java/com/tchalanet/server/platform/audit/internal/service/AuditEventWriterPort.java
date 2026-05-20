package com.tchalanet.server.platform.audit.internal.service;

import java.time.Instant;

public interface AuditEventWriterPort {
  AuditEvent save(AuditEvent event);

  int deleteBefore(Instant threshold);
}
