package com.tchalanet.server.core.subscription.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.core.subscription.application.command.model.SuspendSubscriptionCommand;
import com.tchalanet.server.core.subscription.application.command.model.SuspendSubscriptionResult;
import com.tchalanet.server.core.subscription.application.event.TenantSubscriptionUpdatedEvent;
import com.tchalanet.server.core.subscription.application.port.out.SubscriptionPersistencePort;
import com.tchalanet.server.core.subscription.application.port.out.SubscriptionReaderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;

/**
 * Handler for SuspendSubscriptionCommand.
 */
@UseCase
@RequiredArgsConstructor
public class SuspendSubscriptionCommandHandler
    implements CommandHandler<SuspendSubscriptionCommand, SuspendSubscriptionResult> {

  private final SubscriptionReaderPort readerPort;
  private final SubscriptionPersistencePort persistencePort;
  private final ApplicationEventPublisher eventPublisher;
  private final Clock clock;

  @Override
  @TchTx
  public SuspendSubscriptionResult handle(SuspendSubscriptionCommand cmd) {
    var subscription = readerPort.findByTenantId(cmd.tenantId())
        .orElseThrow(() -> new IllegalArgumentException("Subscription not found for tenant: " + cmd.tenantId()));
    var suspended = subscription.suspend(Instant.now(clock));
    var saved = persistencePort.save(suspended);
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
    return new SuspendSubscriptionResult(saved.id(), saved.status());
  }
}
