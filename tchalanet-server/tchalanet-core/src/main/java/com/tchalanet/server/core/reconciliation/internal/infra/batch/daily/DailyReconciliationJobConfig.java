package com.tchalanet.server.core.reconciliation.internal.infra.batch.daily;

import com.tchalanet.server.common.job.annotation.TchJob;
import com.tchalanet.server.common.job.gate.BatchGate;
import com.tchalanet.server.common.job.key.JobKey;
import com.tchalanet.server.platform.tenant.api.model.TenantStatus;
import com.tchalanet.server.core.reconciliation.internal.application.ReconciliationDailyRunService;
import com.tchalanet.server.core.reconciliation.internal.domain.model.ReconciliationRunType;
import com.tchalanet.server.core.reconciliation.internal.infra.config.ReconciliationDailyJobProperties;
import com.tchalanet.server.core.reconciliation.internal.infra.persistence.ReconciliationRunJpaRepository;
import com.tchalanet.server.platform.tenant.api.TenantConfigApi;
import com.tchalanet.server.platform.tenant.api.model.request.ListTenantsRequest;
import java.time.LocalTime;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailyReconciliationJobConfig {

    private static final JobKey DAILY_RECONCILIATION = JobKey.of("reconciliation:daily:run");

    private final ReconciliationDailyJobProperties properties;
    private final ReconciliationDailyRunService reconciliationDailyRunService;
    private final ReconciliationRunJpaRepository runRepository;
    private final TenantConfigApi tenantConfigApi;
    private final BatchGate batchGate;

    @Scheduled(cron = "${tch.reconciliation.daily.cron:0 */5 * * * *}", zone = "UTC")
    @SchedulerLock(name = "reconciliation_daily_run", lockAtMostFor = "PT30M", lockAtLeastFor = "PT30S")
    @TchJob("reconciliation:daily-run")
    public void tick() {
        if (!properties.isActive()) {
            log.info("reconciliation.daily skipped reason=scheduler_disabled");
            return;
        }
        if (!batchGate.enabled(DAILY_RECONCILIATION, null)) {
            log.info("reconciliation.daily skipped reason=gate_disabled");
            return;
        }

        var tenants = tenantConfigApi.listTenants(new ListTenantsRequest(
                PageRequest.of(0, Math.max(1, properties.getMaxTenantsPerTick()))))
            .stream()
            .filter(tenant -> tenant.status() == TenantStatus.ACTIVE)
            .sorted(Comparator.comparing(tenant -> tenant.code() == null ? "" : tenant.code()))
            .toList();
        for (var tenant : tenants) {
            if (!batchGate.enabled(DAILY_RECONCILIATION, tenant.tenantId())) {
                continue;
            }
            var localNow = java.time.ZonedDateTime.now(tenant.timezone());
            if (!inMidnightWindow(localNow.toLocalTime())) {
                continue;
            }
            var businessDate = localNow.toLocalDate().minusDays(1);
            if (runRepository.existsByTenantIdAndBusinessDateAndRunType(
                tenant.tenantId().value(),
                businessDate,
                ReconciliationRunType.SCHEDULED)) {
                continue;
            }
            try {
                reconciliationDailyRunService.scheduledDailyRun(tenant.tenantId(), businessDate);
            } catch (RuntimeException ex) {
                log.warn("reconciliation.daily tenant failed tenant={} businessDate={}",
                    tenant.code(), businessDate, ex);
            }
        }
    }

    private boolean inMidnightWindow(LocalTime localTime) {
        var end = LocalTime.MIDNIGHT.plusMinutes(Math.max(1, properties.getMidnightWindowMinutes()));
        return !localTime.isBefore(LocalTime.MIDNIGHT) && localTime.isBefore(end);
    }
}
