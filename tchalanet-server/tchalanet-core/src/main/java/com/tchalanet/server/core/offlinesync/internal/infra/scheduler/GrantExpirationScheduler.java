package com.tchalanet.server.core.offlinesync.internal.infra.scheduler;

import com.tchalanet.server.platform.tenant.api.TenantPreContextLookupApi;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.job.context.JobContextBinder;
import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.offlinesync.api.command.grant.ExpireOfflineGrantCommand;
import com.tchalanet.server.core.offlinesync.api.model.grant.OfflineGrantStatus;
import com.tchalanet.server.core.offlinesync.internal.infra.persistence.OfflineGrantJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Marks {@code ACTIVE} grants whose {@code syncAcceptedUntil} is in the past as
 * {@code EXPIRED}. Iterates over active tenants binding the {@link JobContextBinder} so
 * RLS-scoped queries return rows.
 */
@Component
@ConditionalOnProperty(prefix = "tch.offlinesync", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class GrantExpirationScheduler {

    private static final String ACTOR = "offlinesync:grant-expiration";

    private final OfflineGrantJpaRepository grantRepo;
    private final CommandBus commandBus;
    private final TenantPreContextLookupApi tenantRegistry;
    private final JobContextBinder binder;
    private final Clock clock;

    @Scheduled(cron = "${tch.offlinesync.jobs.grant-expiration-cron:0 */5 * * * *}", zone = "UTC")
    @SchedulerLock(
        name = "offlinesync_grant_expiration",
        lockAtMostFor = "PT5M",
        lockAtLeastFor = "PT30S")
    public void tick() {
        MDC.put("job", ACTOR);
        try {
            var now = clock.instant();
            for (TenantId tenantId : tenantRegistry.listActiveTenantIds()) {
                try {
                    binder.bindTenant(tenantId, ACTOR);
                    var expired = grantRepo.findAllByStatusAndSyncAcceptedUntilLessThan(
                        OfflineGrantStatus.ACTIVE.name(), now);
                    for (var e : expired) {
                        try {
                            commandBus.execute(new ExpireOfflineGrantCommand(
                                tenantId, OfflineGrantId.of(e.getId())));
                        } catch (Exception ex) {
                            log.warn("offlinesync: failed to expire grant {} tenant={} — {}",
                                e.getId(), tenantId, ex.getMessage());
                        }
                    }
                } catch (Exception ex) {
                    log.warn("offlinesync: grant-expiration tenant {} failed — {}",
                        tenantId, ex.getMessage());
                } finally {
                    binder.clear();
                }
            }
        } finally {
            MDC.remove("job");
        }
    }
}
