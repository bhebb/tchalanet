package com.tchalanet.server.platform.clientdiagnostics.internal.service;

import com.tchalanet.server.platform.clientdiagnostics.internal.persistence.ClientDiagnosticsJdbcRepository;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ClientDiagnosticsRetentionJob {

  private static final Duration RETENTION = Duration.ofDays(7);

  private final ClientDiagnosticsJdbcRepository repository;

  @Scheduled(cron = "0 17 3 * * *")
  void purgeExpiredEvents() {
    repository.purgeEventsOlderThan(RETENTION);
  }
}
