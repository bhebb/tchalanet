package com.tchalanet.server.core.audit.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.audit.application.command.model.PurgeOldAuditEventsCommand;
import com.tchalanet.server.core.audit.application.port.out.AuditEventWriterPort;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class PurgeOldAuditEventsCommandHandler
    implements VoidCommandHandler<PurgeOldAuditEventsCommand> {

  private final AuditEventWriterPort repository;
  private final Clock clock;

  @Value("${tch.audit.retention-days:90}")
  private int retentionDays;

  @Override
  @TchTx
  public void handle(PurgeOldAuditEventsCommand command) {
    var threshold = Instant.now(clock).minus(retentionDays, ChronoUnit.DAYS);
    int deleted = repository.deleteBefore(threshold);
    log.info("Purged {} audit events older than {} days (threshold={})", deleted, retentionDays, threshold);
  }
}
