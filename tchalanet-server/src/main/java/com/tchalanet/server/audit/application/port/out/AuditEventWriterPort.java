package com.tchalanet.server.audit.application.port.out;

import com.tchalanet.server.audit.domain.model.AuditEvent;
import java.time.Instant;

public interface AuditEventWriterPort {
  AuditEvent save(AuditEvent event);

  int deleteBefore(Instant threshold);
}
