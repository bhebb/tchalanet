package com.tchalanet.server.core.draw.infra.batch.config;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.core.draw.application.command.model.CloseDueDrawsCommand;
import com.tchalanet.server.core.draw.application.command.model.GenerateDrawsForRangeCommand;
import com.tchalanet.server.core.draw.application.command.model.OpenDueDrawsCommand;
import com.tchalanet.server.core.draw.application.port.out.TenantDrawCalendarQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Component
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


    @Scheduled(cron = "0 0 5 * * *", zone = "America/Toronto")
    public void generateNext7Days() {
        var from = LocalDate.now(DEFAULT_TENANT_ZONE);
        var to = from.plusDays(7);
        for (UUID tenantId : tenantPort.listActiveTenantIdsForDrawCalendar()) {
            try {
                commandBus.send(new GenerateDrawsForRangeCommand(tenantId, from, to, DEFAULT_DRY_RUN, DEFAULT_DRY_RUN));
            } catch (Exception e) {
                log.warn("generateNext7Days failed for tenantId={}", tenantId, e);
            }
        }
    }

    @Scheduled(cron = "0 * * * * *")
    public void openDueDraws() {
        var now = Instant.now();
        commandBus.send(
            new OpenDueDrawsCommand(
                now, DEFAULT_LIMIT, OPEN_HORIZON_HOURS, OPEN_LAG_HOURS, DEFAULT_DRY_RUN));
    }

    @Scheduled(cron = "30 * * * * *")
    public void closeDueDraws() {
        var now = Instant.now();
        commandBus.send(new CloseDueDrawsCommand(now, DEFAULT_LIMIT, DEFAULT_DRY_RUN));
    }
}
