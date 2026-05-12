package com.tchalanet.server.core.subscription.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.core.subscription.api.command.RenewSubscriptionCommand;
import com.tchalanet.server.core.subscription.api.command.RenewSubscriptionResult;
import com.tchalanet.server.core.subscription.internal.application.event.TenantSubscriptionUpdatedEvent;
import com.tchalanet.server.core.subscription.internal.application.port.out.SubscriptionPersistencePort;
import com.tchalanet.server.core.subscription.internal.application.port.out.SubscriptionReaderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;

/**
 * Handler for RenewSubscriptionCommand.
 */
@UseCase
@RequiredArgsConstructor
public class RenewSubscriptionCommandHandler
    implements CommandHandler<RenewSubscriptionCommand, RenewSubscriptionResult> {

  private final SubscriptionReaderPort readerPort;
  private final SubscriptionPersistencePort persistencePort;
  private final ApplicationEventPublisher eventPublisher;
  private final Clock clock;

  @Override
  @TchTx
  public RenewSubscriptionResult handle(RenewSubscriptionCommand cmd) {
    // 1. Read existing subscription
    var subscription = readerPort.findByTenantId(cmd.tenantId())
        .orElseThrow(() -> new IllegalArgumentException("Subscription not found for tenant: " + cmd.tenantId()));

    // 2. Renew via domain method
    var renewed = subscription.renew(cmd.newEndsAt());

    // 3. Persist
    var saved = persistencePort.save(renewed);

    // 4. Publish event after-commit
    AfterCommit.run(() -> {
      eventPublisher.publishEvent(new TenantSubscriptionUpdatedEvent(
          saved.tenantId(),
          saved.planCode(),
          saved.status(),
          saved.version(),
          Instant.now(clock),
          "system"
      ));
    });

    return new RenewSubscriptionResult(saved.id(), saved.endsAt());
  }
}
