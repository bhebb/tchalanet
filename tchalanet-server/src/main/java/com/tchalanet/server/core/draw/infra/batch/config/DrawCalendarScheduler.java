package com.tchalanet.server.core.draw.infra.batch.config;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.application.command.model.CloseDueDrawsCommand;
import com.tchalanet.server.core.draw.application.command.model.GenerateDrawsForRangeCommand;
import com.tchalanet.server.core.draw.application.command.model.OpenDueDrawsCommand;
import com.tchalanet.server.core.draw.application.port.out.TenantDrawCalendarQueryPort;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class DrawCalendarScheduler {

  private static final ZoneId DEFAULT_TENANT_ZONE = ZoneId.of("America/Toronto");
  private static final int DEFAULT_LIMIT = 2000;
  private static final int OPEN_HORIZON_HOURS = 12;
  private static final int OPEN_LAG_HOURS = 6;
  private static final boolean DEFAULT_DRY_RUN = false;

  private final TenantDrawCalendarQueryPort tenantPort;
  private final CommandBus commandBus;

  // Guard to allow disabling scheduled methods per-profile (kept for runtime safety)
  @Value("${app.scheduling.enabled:true}")
  private boolean appSchedulingEnabled;

  @PostConstruct
  public void postConstruct() {
    log.info("DrawCalendarScheduler created: app.scheduling.enabled={}", appSchedulingEnabled);
  }

  @Scheduled(cron = "0 0 5 * * *", zone = "America/Toronto")
  public void generateNext7Days() {
    if (!appSchedulingEnabled) return;

    var from = LocalDate.now(DEFAULT_TENANT_ZONE);
    var to = from.plusDays(7);
    for (TenantId tenantId : tenantPort.listActiveTenantIdsForDrawCalendar()) {
      try {
        commandBus.send(
            new GenerateDrawsForRangeCommand(tenantId, from, to, DEFAULT_DRY_RUN, DEFAULT_DRY_RUN));
      } catch (Exception e) {
        log.warn("generateNext7Days failed for tenantId={}", tenantId, e);
      }
    }
  }

  @Scheduled(cron = "0 * * * * *")
  public void openDueDraws() {
    if (!appSchedulingEnabled) return;

    var now = Instant.now();
    commandBus.send(
        new OpenDueDrawsCommand(
            now, DEFAULT_LIMIT, OPEN_HORIZON_HOURS, OPEN_LAG_HOURS, DEFAULT_DRY_RUN));
  }

  @Scheduled(cron = "30 * * * * *")
  public void closeDueDraws() {
    if (!appSchedulingEnabled) return;

    var now = Instant.now();
    commandBus.send(new CloseDueDrawsCommand(now, DEFAULT_LIMIT, DEFAULT_DRY_RUN));
  }
}
