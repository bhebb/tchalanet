package com.tchalanet.server.core.pos.application;

import com.tchalanet.server.core.pos.domain.model.PosSession;
import com.tchalanet.server.core.pos.domain.ports.in.AutoCloseSessionsUseCase;
import com.tchalanet.server.core.pos.domain.ports.out.PosSessionEventPublisherPort;
import com.tchalanet.server.core.pos.domain.ports.out.PosSessionRepositoryPort;
import com.tchalanet.server.core.sales.domain.ports.out.ClockPort;
import com.tchalanet.server.core.tenantconfig.domain.ports.in.GetTenantConfigUseCase; // New import
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutoCloseSessionsService implements AutoCloseSessionsUseCase {

  private final PosSessionRepositoryPort sessionRepository;
  private final PosSessionEventPublisherPort eventPublisher;
  private final ClockPort clock;
  private final GetTenantConfigUseCase tenantConfigUseCase; // Injected TenantConfig use case

  // Default configuration values (used if not found in TenantConfig)
  private static final long DEFAULT_MAX_SESSION_DURATION_SECONDS = 16 * 3600; // 16 hours
  private static final long DEFAULT_IDLE_TIMEOUT_SECONDS = 2 * 3600; // 2 hours
  private static final String DEFAULT_AUTO_CLOSE_TIME = "23:59"; // HH:mm

  @Override
  @Transactional
  public int autoCloseSessions(UUID tenantId) {
    log.info(
        "Starting auto-close session job for tenant {}",
        tenantId != null ? tenantId : "all tenants");
    Instant now = clock.now();

    // In a multi-tenant system, you would typically iterate over all active tenants
    // and call autoCloseSessions(tenantId) for each.
    // For simplicity, this example fetches all open sessions and applies config per session.
    // A more robust solution would fetch tenant configs first, then filter sessions.

    List<PosSession> sessionsToConsider =
        sessionRepository.findOpenSessions(
            now.minusSeconds(DEFAULT_IDLE_TIMEOUT_SECONDS),
            now.minusSeconds(DEFAULT_MAX_SESSION_DURATION_SECONDS));

    int closedCount = 0;
    for (PosSession session : sessionsToConsider) {
      // Fetch tenant-specific configurations
      long maxSessionDurationSeconds =
          tenantConfigUseCase
              .getInteger(session.getTenantId(), "pos.session.max_duration_hours")
              .map(h -> h * 3600L)
              .orElse(DEFAULT_MAX_SESSION_DURATION_SECONDS);
      long idleTimeoutSeconds =
          tenantConfigUseCase
              .getInteger(session.getTenantId(), "pos.session.idle_timeout_minutes")
              .map(m -> m * 60L)
              .orElse(DEFAULT_IDLE_TIMEOUT_SECONDS);
      String autoCloseTimeStr =
          tenantConfigUseCase
              .getString(session.getTenantId(), "pos.session.auto_close_time")
              .orElse(DEFAULT_AUTO_CLOSE_TIME);

      Instant sessionIdleCutoff = now.minusSeconds(idleTimeoutSeconds);
      Instant sessionOpenedCutoff = now.minusSeconds(maxSessionDurationSeconds);

      boolean shouldClose = false;

      // Check idle timeout
      if (session.getLastActivityAt().isBefore(sessionIdleCutoff)) {
        log.debug(
            "Session {} for terminal {} (tenant {}) is idle. Last activity: {}",
            session.getId(),
            session.getTerminalId(),
            session.getTenantId(),
            session.getLastActivityAt());
        shouldClose = true;
      }

      // Check max duration
      if (session.getOpenedAt().isBefore(sessionOpenedCutoff)) {
        log.debug(
            "Session {} for terminal {} (tenant {}) exceeded max duration. Opened at: {}",
            session.getId(),
            session.getTerminalId(),
            session.getTenantId(),
            session.getOpenedAt());
        shouldClose = true;
      }

      // Check auto_close_at_time (e.g., end of day)
      // This requires knowing the tenant's timezone, which should also come from TenantConfig or
      // Tenant domain
      // For now, using a placeholder ZoneId
      ZoneId tenantZone = ZoneId.of("America/Port-au-Prince"); // Placeholder
      LocalTime autoCloseTime = LocalTime.parse(autoCloseTimeStr);
      LocalTime currentTimeInTenantZone = now.atZone(tenantZone).toLocalTime();

      if (currentTimeInTenantZone.isAfter(autoCloseTime)
          && session
              .getOpenedAt()
              .atZone(tenantZone)
              .toLocalDate()
              .isBefore(now.atZone(tenantZone).toLocalDate())) {
        log.debug(
            "Session {} for terminal {} (tenant {}) is past auto-close time for the day. Auto-close time: {}",
            session.getId(),
            session.getTerminalId(),
            session.getTenantId(),
            autoCloseTime);
        shouldClose = true;
      }

      if (shouldClose) {
        try {
          session.autoClose();
          sessionRepository.save(session);
          eventPublisher.publishSessionClosedEvent(
              session.getId(),
              session.getTenantId(),
              session.getTerminalId(),
              session.getUserId(),
              "AUTO");
          closedCount++;
          log.info(
              "Auto-closed session {} for terminal {} (tenant {})",
              session.getId(),
              session.getTerminalId(),
              session.getTenantId());
        } catch (Exception e) {
          log.error("Failed to auto-close session {}: {}", session.getId(), e.getMessage(), e);
        }
      }
    }

    log.info("Finished auto-close session job. {} sessions closed.", closedCount);
    return closedCount;
  }
}
