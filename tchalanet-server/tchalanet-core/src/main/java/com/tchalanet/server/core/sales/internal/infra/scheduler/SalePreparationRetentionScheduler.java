package com.tchalanet.server.core.sales.internal.infra.scheduler;

import com.tchalanet.server.common.job.context.JobContextBinder;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.sales.api.model.preparation.SalePreparationStatus;
import com.tchalanet.server.core.sales.internal.infra.persistence.repository.SalePreparationRepository;
import com.tchalanet.server.platform.tenant.api.TenantPreContextLookupApi;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * SalePreparation retention (DOMAIN_SALES.md §11) :
 * - expire les DRAFT au-delà du TTL (filet du check paresseux au confirm/regenerate) ;
 * - purge EXPIRED/CANCELLED après 7 jours ;
 * - purge CONFIRMED après 30 jours (le ticket persisté reste la vérité).
 */
@Component
@ConditionalOnProperty(prefix = "tch.sales.preparation", name = "retention-enabled",
    havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class SalePreparationRetentionScheduler {

    private static final String ACTOR = "sales:preparation-retention";
    private static final Duration TERMINAL_RETENTION = Duration.ofDays(7);
    private static final Duration CONFIRMED_RETENTION = Duration.ofDays(30);

    private final SalePreparationRepository preparationRepository;
    private final TenantPreContextLookupApi tenantRegistry;
    private final JobContextBinder binder;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    @Scheduled(cron = "${tch.sales.preparation.retention-cron:0 */5 * * * *}", zone = "UTC")
    @SchedulerLock(
        name = "sales_preparation_retention",
        lockAtMostFor = "PT5M",
        lockAtLeastFor = "PT30S")
    public void tick() {
        MDC.put("job", ACTOR);
        try {
            var now = clock.instant();
            for (TenantId tenantId : tenantRegistry.listActiveTenantIds()) {
                try {
                    binder.bindTenant(tenantId, ACTOR);
                    transactionTemplate.executeWithoutResult(tx -> runForTenant(now));
                } catch (Exception ex) {
                    log.warn("sales: preparation retention tenant {} failed — {}",
                        tenantId, ex.getMessage());
                } finally {
                    binder.clear();
                }
            }
        } finally {
            MDC.remove("job");
        }
    }

    private void runForTenant(java.time.Instant now) {
        int expired = preparationRepository.expireDrafts(now);
        int purgedTerminal = preparationRepository.purgeByStatusOlderThan(
            List.of(SalePreparationStatus.EXPIRED, SalePreparationStatus.CANCELLED),
            now.minus(TERMINAL_RETENTION));
        int purgedConfirmed = preparationRepository.purgeByStatusOlderThan(
            List.of(SalePreparationStatus.CONFIRMED),
            now.minus(CONFIRMED_RETENTION));
        if (expired > 0 || purgedTerminal > 0 || purgedConfirmed > 0) {
            log.info("sales: preparation retention expired={} purgedTerminal={} purgedConfirmed={}",
                expired, purgedTerminal, purgedConfirmed);
        }
    }
}
