package com.tchalanet.server.core.draw.infra.batch.config;

import com.tchalanet.server.common.batch.BatchGate;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.config.batch.BatchWindowsConfig;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.application.command.model.CloseDueDrawsCommand;
import com.tchalanet.server.core.draw.application.command.model.GenerateDrawsForRangeCommand;
import com.tchalanet.server.core.draw.application.command.model.OpenDueDrawsCommand;
import com.tchalanet.server.core.draw.application.port.out.TenantDrawCalendarQueryPort;
import jakarta.annotation.PostConstruct;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DrawCalendarScheduler {


    private static final ZoneId DEFAULT_TENANT_ZONE = ZoneId.of("America/New_York");
    private static final boolean DEFAULT_DRY_RUN = false;

    private final TenantDrawCalendarQueryPort tenantPort;
    private final CommandBus commandBus;
    private final BatchGate batchGate;
    private final BatchWindowsConfig windows;

    @Scheduled(cron = "0 0 5 * * *", zone = "America/New_York")
    public void generateNext7Days() {
        if (!batchGate.canRun("generate")) {
            log.info("batch.skip job=generate reason=disabled");
            return;
        }

        var from = LocalDate.now(DEFAULT_TENANT_ZONE);
        var to = from.plusDays(7);

        for (TenantId tenantId : tenantPort.listActiveTenantIdsForDrawCalendar()) {
            try {
                commandBus.send(new GenerateDrawsForRangeCommand(tenantId, from, to, DEFAULT_DRY_RUN, false));
            } catch (Exception e) {
                log.warn("generateNext7Days failed for tenantId={} from={} to={}", tenantId, from, to, e);
            }
        }
    }

    @Scheduled(cron = "0 0 2 * * *", zone = "America/New_York")
    public void openDueDrawsNightly() {
        if (!batchGate.canRun("open")) {
            log.info("batch.skip job=open reason=disabled");
            return;
        }
        commandBus.send(new OpenDueDrawsCommand(Instant.now(), 5000, 24, 12, false));
    }

    // OPEN: toutes les 30 min pendant une fenêtre courte la nuit/matin
    @Scheduled(cron = "0 */30 * * * *", zone = "America/New_York")
    public void openWindowed() {
        if (!batchGate.canRun("open")) {
            log.info("batch.skip job=open reason=disabled");
            return;
        }
        var nowLocal = ZonedDateTime.now(DEFAULT_TENANT_ZONE).toLocalTime();
        if (!windows.isInOpenDrawsWindow(nowLocal)) return;

        commandBus.send(new OpenDueDrawsCommand(
            Instant.now(),
            5000,
            24, // horizon
            12, // lag
            false
        ));
    }

    // CLOSE: toutes les 10-15 min dans les fenêtres de tirage
    @Scheduled(cron = "0 */15 * * * *", zone = "America/New_York")
    public void closeWindowed() {
        if (!batchGate.canRun("close")) {
            log.info("batch.skip job=close reason=disabled");
            return;
        }
        var nowLocal = java.time.ZonedDateTime.now(java.time.ZoneId.of("America/New_York")).toLocalTime();
        if (!windows.isInCloseDrawsWindow(nowLocal)) return;

        commandBus.send(new CloseDueDrawsCommand(
            Instant.now(),
            5000,
            false
        ));
    }

}
