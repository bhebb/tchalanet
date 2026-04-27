package com.tchalanet.server.core.audit.application.port.out;

import com.tchalanet.server.core.audit.domain.model.AuditEvent;
import java.time.Instant;

public interface AuditEventWriterPort {
  AuditEvent save(AuditEvent event);

  int deleteBefore(Instant threshold);
}
