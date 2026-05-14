package com.tchalanet.server.platform.audit.internal.service;

import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.platform.audit.api.model.request.PurgeOldAuditEventsRequest;
import com.tchalanet.server.platform.audit.api.model.PurgeOldAuditEventsResult;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class PurgeOldAuditEventsCommandHandler {

  private final AuditEventWriterPort repository;
  private final Clock clock;

  @Value("${tch.audit.retention-days:90}")
  private int retentionDays;

  @TchTx
  public PurgeOldAuditEventsResult handle(PurgeOldAuditEventsRequest command) {
    var threshold = Instant.now(clock).minus(retentionDays, ChronoUnit.DAYS);
    int deleted = repository.deleteBefore(threshold);
    log.info("Purged {} audit events older than {} days (threshold={})", deleted, retentionDays, threshold);
    return new PurgeOldAuditEventsResult(deleted, retentionDays, threshold);
  }
}
