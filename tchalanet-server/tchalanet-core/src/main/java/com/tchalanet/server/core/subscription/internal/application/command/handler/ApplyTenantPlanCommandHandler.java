package com.tchalanet.server.core.subscription.internal.application.command.handler;

import com.tchalanet.server.catalog.plan.api.PlanCatalog;
import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.SubscriptionId;
import com.tchalanet.server.core.subscription.api.command.ApplyTenantPlanCommand;
import com.tchalanet.server.core.subscription.api.command.ApplyTenantPlanResult;
import com.tchalanet.server.core.subscription.api.event.TenantSubscriptionUpdatedEvent; // Keep for now, might be used elsewhere
import com.tchalanet.server.core.subscription.internal.application.port.out.SubscriptionPersistencePort;
import com.tchalanet.server.core.subscription.internal.application.port.out.SubscriptionReaderPort;
import com.tchalanet.server.core.subscription.internal.domain.model.Subscription;
import com.tchalanet.server.core.subscription.internal.domain.model.SubscriptionStatus;
import com.tchalanet.server.platform.entitlement.api.EntitlementCacheInvalidationApi;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher; // Keep for now, might be used elsewhere

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

/**
 * Handler for ApplyTenantPlanCommand.
 * Maps to spec requirement S1 (apply plan to tenant).
 * <p>
 * CRITICAL per REFACTORING_GUIDE.md and spec S6:
 * - Validates plan via PlanCatalog.findByCode() (public API only)
 * - NEVER accesses catalog/plan/internal or PlanJpaRepository
 * - Uses planCode (String), not PlanId
 */
@UseCase
@RequiredArgsConstructor
public class ApplyTenantPlanCommandHandler
    implements CommandHandler<ApplyTenantPlanCommand, ApplyTenantPlanResult> {

    private final PlanCatalog planCatalog; // ✅ API publique catalog/plan
    private final SubscriptionPersistencePort persistencePort;
    private final SubscriptionReaderPort readerPort;
    private final ApplicationEventPublisher eventPublisher; // Keep for now, if other events are published
    private final Clock clock;
    private final EntitlementCacheInvalidationApi entitlementCacheInvalidationApi;

    @Override
    @TchTx
    public ApplyTenantPlanResult handle(ApplyTenantPlanCommand cmd) {
        // 1. Validate plan via PlanCatalog (public API) - spec S1 + S6
        var plan = planCatalog.findByCode(cmd.planCode())
            .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + cmd.planCode()));

        if (!plan.active()) {
            // Policy: reject inactive plans for new assignments (per spec S1 scenario "plan inactive policy")
            throw new IllegalArgumentException("Plan is inactive: " + cmd.planCode());
        }

        // 2. Read existing subscription
        var existing = readerPort.findByTenantId(cmd.tenantId());

        // 3. Create/update subscription
        long newVersion = existing.map(s -> s.version() + 1).orElse(1L);
        Instant now = Instant.now(clock);
        Instant effectiveAt = cmd.effectiveAt() != null ? cmd.effectiveAt() : now;

        var subscription = new Subscription(
            existing.map(Subscription::id).orElse(SubscriptionId.of(UUID.randomUUID())),
            cmd.tenantId(),
            cmd.planCode(), // ✅ string soft reference
            SubscriptionStatus.ACTIVE,
            effectiveAt,
            null, // ends_at - to be set by renewal logic
            null, // trial_ends_at
            null, // canceled_at
            new HashMap<>(), // metadata
            newVersion,
            existing.map(Subscription::createdAt).orElse(now),
            now,
            "system" // TODO: get from security context
        );

        var saved = persistencePort.save(subscription);

        // 4. Publish event after-commit (spec S5) and evict cache
        AfterCommit.run(() -> {
            entitlementCacheInvalidationApi.evictTenantSnapshot(cmd.tenantId());
            // If TenantSubscriptionUpdatedEvent is still needed for other listeners, keep this line.
            // Otherwise, it can be removed. Assuming it might be needed for other purposes.
            eventPublisher.publishEvent(new TenantSubscriptionUpdatedEvent(
                saved.tenantId(),
                saved.planCode(),
                saved.status(),
                saved.version(),
                Instant.now(clock),
                "system"
            ));
        });

        return new ApplyTenantPlanResult(saved.id(), saved.status());
    }
}
