package com.tchalanet.server.platform.archive.internal.config;

import com.tchalanet.server.platform.archive.internal.persistence.ArchiveObjectJdbcRepository;
import com.tchalanet.server.platform.archive.internal.persistence.ArchiveRunJdbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Spring Boot health indicator for the archive subsystem.
 *
 * <p>Reports DOWN if there are FAILED archive runs or INVALID archive objects,
 * which both indicate conditions requiring operator attention.
 */
@Component("archive")
@RequiredArgsConstructor
public class ArchiveHealthIndicator implements HealthIndicator {

  private final ArchiveRunJdbcRepository runRepo;
  private final ArchiveObjectJdbcRepository objectRepo;

  @Override
  public Health health() {
    long failedRuns     = runRepo.countByStatus("FAILED");
    long invalidObjects = objectRepo.countByStatus("INVALID");

    Health.Builder builder = (failedRuns > 0 || invalidObjects > 0)
        ? Health.down()
        : Health.up();

    return builder
        .withDetail("failedRuns",     failedRuns)
        .withDetail("invalidObjects", invalidObjects)
        .build();
  }
}
