package com.tchalanet.server.common.audit.domain.usecase.impl;

import com.tchalanet.server.common.audit.domain.ports.AuditEventRepository;
import com.tchalanet.server.common.audit.domain.usecase.PurgeOldAuditEventsUseCase;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PurgeOldAuditEventsUseCaseImpl implements PurgeOldAuditEventsUseCase {

  private final AuditEventRepository repository;

  @Value("${tch.audit.retention-days:90}")
  private int retentionDays;

  @Override
  @Transactional
  public void purge() {
    Instant threshold = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
    int deleted = repository.deleteBefore(threshold);
    log.info("Purged {} audit events older than {} days", deleted, retentionDays);
  }
}
