package com.tchalanet.server.audit.application.command.handler;

import com.tchalanet.server.audit.application.command.model.PurgeOldAuditEventsCommand;
import com.tchalanet.server.audit.application.port.in.PurgeOldAuditEventsCommandHandler;
import com.tchalanet.server.audit.application.port.out.AuditEventWriterPort;
import com.tchalanet.server.common.domain.UseCase;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class PurgeOldAuditEventsUseCase implements PurgeOldAuditEventsCommandHandler {

  private final AuditEventWriterPort repository;

  @Value("${tch.audit.retention-days:90}")
  private int retentionDays;

  @Override
  // add transaction transverse
  public void handle(PurgeOldAuditEventsCommand command) {
    var threshold = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
    int deleted = repository.deleteBefore(threshold);
    log.info("Purged {} audit events older than {} days", deleted, retentionDays);
  }
}
