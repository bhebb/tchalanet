package com.tchalanet.server.audit.application.service;

import com.tchalanet.server.audit.domain.model.AuditEvent;
import com.tchalanet.server.audit.domain.ports.out.AuditEventWriterPort;
import com.tchalanet.server.audit.domain.usecase.LogAuditEventUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLoggingService implements LogAuditEventUseCase {

  private final AuditEventWriterPort writer;

  @Override
  public void log(AuditEvent event) {
    if (event == null) return;
    try {
      writer.save(event);
    } catch (Exception e) {
      // Do not fail caller because of audit; just log (we don't have logger here to avoid lombok
      // use)
      log.error("Failed to write audit event: {}", e.getMessage());
    }
  }
}
