package com.tchalanet.server.core.subscription.internal.application.command.handler;

import com.tchalanet.server.catalog.plan.api.PlanCatalog;
import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.core.subscription.api.command.ChangePlanCommand;
import com.tchalanet.server.core.subscription.api.command.ChangePlanResult;
import com.tchalanet.server.core.subscription.api.event.TenantSubscriptionUpdatedEvent;
import com.tchalanet.server.core.subscription.internal.application.port.out.SubscriptionPersistencePort;
import com.tchalanet.server.core.subscription.internal.application.port.out.SubscriptionReaderPort;
import com.tchalanet.server.platform.entitlement.api.EntitlementCacheInvalidationApi;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;

/**
 * Handler for ChangePlanCommand.
 * Maps to SUBSCRIPTION_COMMANDS.md (ChangePlanCommand).
 * Validates new plan via PlanCatalog (public API).
 */
@UseCase
@RequiredArgsConstructor
public class ChangePlanCommandHandler
    implements CommandHandler<ChangePlanCommand, ChangePlanResult> {

    private final PlanCatalog planCatalog; // ✅ API publique
    private final SubscriptionReaderPort readerPort;
    private final SubscriptionPersistencePort persistencePort;
    private final ApplicationEventPublisher eventPublisher;
    private final EntitlementCacheInvalidationApi entitlementCacheInvalidationApi;
    private final Clock clock;

    @Override
    @TchTx
    public ChangePlanResult handle(ChangePlanCommand cmd) {
        // 1. Validate new plan via PlanCatalog
        var newPlan = planCatalog.findByCode(cmd.newPlanCode())
            .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + cmd.newPlanCode()));

        if (!newPlan.active()) {
            throw new IllegalArgumentException("Plan is inactive: " + cmd.newPlanCode());
        }

        // 2. Read existing subscription
        var subscription = readerPort.findByTenantId(cmd.tenantId())
            .orElseThrow(() -> new IllegalArgumentException("Subscription not found for tenant: " + cmd.tenantId()));

        String oldPlanCode = subscription.planCode();

        // 3. Change plan via domain method
        Instant now = Instant.now(clock);
        var changed = subscription.changePlan(cmd.newPlanCode(), now);

        // 4. Persist
        var saved = persistencePort.save(changed);

        // 5. Publish event after-commit
        AfterCommit.run(() -> {
            entitlementCacheInvalidationApi.evictTenantSnapshot(cmd.tenantId());
            eventPublisher.publishEvent(new TenantSubscriptionUpdatedEvent(
                saved.tenantId(),
                saved.planCode(),
                saved.status(),
                saved.version(),
                Instant.now(clock),
                "system"
            ));
        });

        return new ChangePlanResult(saved.id(), oldPlanCode, saved.planCode());
    }
}
