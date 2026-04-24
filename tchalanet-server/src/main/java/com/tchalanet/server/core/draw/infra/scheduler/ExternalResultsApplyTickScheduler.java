package com.tchalanet.server.core.draw.infra.scheduler;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.catalog.tenant.api.TenantCatalog;
import com.tchalanet.server.common.batch.context.BatchTchContextBinder;
import com.tchalanet.server.common.batch.gate.BatchGate;
import com.tchalanet.server.common.batch.key.BatchJobKeys;
import com.tchalanet.server.common.batch.params.BatchParamKeys;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.application.command.model.ApplyExternalResultsWindowCommand;
import com.tchalanet.server.core.drawresult.infra.config.DrawResultsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalResultsApplyTickScheduler {

    private final CommandBus commandBus;
    private final BatchGate gate;
    private final TenantCatalog tenantCatalog;
    private final ResultSlotCatalog resultSlotCatalog;
    private final DrawResultsProperties props;
    private final Clock clock;
    private final BatchTchContextBinder binder;

    // MVP single-instance guard against overlapping ticks
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(cron = "${tch.draw.results.scheduler.apply_cron:30 */5 * * * *}")
    public void tickApply() {
        if (!props.isActive() || !props.getScheduler().isActive()) {
            log.debug("draw-results.apply.tick: active=OFF");
            return;
        }
        if (!gate.enabled(BatchJobKeys.RESULTS_EXTERNAL_APPLY, null)) {
            log.debug("draw-results.apply.tick: gate=OFF");
            return;
        }
        if (!running.compareAndSet(false, true)) {
            log.debug("draw-results.apply.tick: skipped (already running)");
            return;
        }

        try {
            var tenants = tenantCatalog.listActiveTenantIds();
            if (tenants == null || tenants.isEmpty()) {
                log.debug("draw-results.apply.tick: tenants=0");
                return;
            }

            var slots = resultSlotCatalog.listActive().stream()
                .filter(s -> s.timezone() != null && s.drawTime() != null)
                .toList();

            if (slots.isEmpty()) {
                log.debug("draw-results.apply.tick: slots=0");
                return;
            }

            for (TenantId tenantId : tenants) {
                var now = Instant.now(clock);
                var requestId = "tick-" + now.toEpochMilli() + "-" + UUID.randomUUID();

                var jp = new JobParametersBuilder()
                    .addString(BatchParamKeys.TENANT_ID, tenantId.value().toString())
                    .addString(BatchParamKeys.REQUEST_ID, requestId)
                    .addString(BatchParamKeys.ACTOR, "scheduler")
                    .toJobParameters();

                try {
                    binder.bind(jp);

                    for (var slot : slots) {
                        var baseDate = LocalDate.now(clock.withZone(slot.timezone()));

                        commandBus.send(new ApplyExternalResultsWindowCommand(
                            tenantId,
                            baseDate,
                            0,
                            List.of(slot.slotKey()),
                            false,
                            false,
                            1
                        ));
                    }

                    log.info("draw-results.apply.tick: tenant={}   requestId={}", tenantId, requestId);

                } catch (Exception e) {
                    log.warn("draw-results.apply.tick: tenant={} failed: {}", tenantId, e.toString());
                } finally {
                    try {
                        binder.clear();
                    } catch (Exception ex) {
                        log.warn("draw-results.apply.tick: tenant={} failed to clear context: {}", tenantId, ex.toString());
                    }
                }

            }

        } finally {
            running.set(false);
        }
    }
}
