package com.tchalanet.server.core.subscription.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.core.subscription.api.command.ResumeSubscriptionCommand;
import com.tchalanet.server.core.subscription.api.command.ResumeSubscriptionResult;
import com.tchalanet.server.core.subscription.api.event.TenantSubscriptionUpdatedEvent;
import com.tchalanet.server.core.subscription.internal.application.port.out.SubscriptionPersistencePort;
import com.tchalanet.server.core.subscription.internal.application.port.out.SubscriptionReaderPort;
import com.tchalanet.server.platform.entitlement.api.EntitlementCacheInvalidationApi;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;

/**
 * Handler for ResumeSubscriptionCommand.
 */
@UseCase
@RequiredArgsConstructor
public class ResumeSubscriptionCommandHandler
    implements CommandHandler<ResumeSubscriptionCommand, ResumeSubscriptionResult> {

    private final SubscriptionReaderPort readerPort;
    private final SubscriptionPersistencePort persistencePort;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;
    private final EntitlementCacheInvalidationApi entitlementCacheInvalidationApi;

    @Override
    @TchTx
    public ResumeSubscriptionResult handle(ResumeSubscriptionCommand cmd) {
        var subscription = readerPort.findByTenantId(cmd.tenantId())
            .orElseThrow(() -> new IllegalArgumentException("Subscription not found for tenant: " + cmd.tenantId()));
        Instant now = Instant.now(clock);
        var resumed = subscription.resume(now);
        var saved = persistencePort.save(resumed);
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
        return new ResumeSubscriptionResult(saved.id());
    }
}
