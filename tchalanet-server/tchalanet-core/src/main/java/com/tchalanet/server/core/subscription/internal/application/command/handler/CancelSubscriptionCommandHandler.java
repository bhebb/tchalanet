package com.tchalanet.server.core.subscription.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.core.subscription.api.command.CancelSubscriptionCommand;
import com.tchalanet.server.core.subscription.api.command.CancelSubscriptionResult;
import com.tchalanet.server.core.subscription.api.event.TenantSubscriptionCanceledEvent;
import com.tchalanet.server.core.subscription.internal.application.port.out.SubscriptionPersistencePort;
import com.tchalanet.server.core.subscription.internal.application.port.out.SubscriptionReaderPort;
import com.tchalanet.server.platform.entitlement.api.EntitlementCacheInvalidationApi;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;

/**
 * Handler for CancelSubscriptionCommand.
 * Maps to SUBSCRIPTION_COMMANDS.md (CancelSubscriptionCommand).
 */
@UseCase
@RequiredArgsConstructor
public class CancelSubscriptionCommandHandler
    implements CommandHandler<CancelSubscriptionCommand, CancelSubscriptionResult> {

    private final SubscriptionReaderPort readerPort;
    private final SubscriptionPersistencePort persistencePort;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;
    private final EntitlementCacheInvalidationApi entitlementCacheInvalidationApi;

    @Override
    @TchTx
    public CancelSubscriptionResult handle(CancelSubscriptionCommand cmd) {
        // 1. Read existing subscription
        var subscription = readerPort.findByTenantId(cmd.tenantId())
            .orElseThrow(() -> new IllegalArgumentException("Subscription not found for tenant: " + cmd.tenantId()));

        // 2. Cancel via domain method
        Instant now = Instant.now(clock);
        var canceled = subscription.cancelNow(now);

        // 3. Persist
        var saved = persistencePort.save(canceled);

        // 4. Publish event after-commit and evict cache
        AfterCommit.run(() -> {
            entitlementCacheInvalidationApi.evictTenantSnapshot(cmd.tenantId());

            eventPublisher.publishEvent(new TenantSubscriptionCanceledEvent(
                saved.tenantId(),
                saved.planCode(),
                cmd.reason(),
                saved.canceledAt(),
                saved.version(),
                Instant.now(clock),
                "system"
            ));
        });

        return new CancelSubscriptionResult(saved.id());
    }
}
