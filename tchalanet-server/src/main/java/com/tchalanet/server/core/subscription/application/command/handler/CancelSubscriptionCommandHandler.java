package com.tchalanet.server.core.subscription.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.core.subscription.application.command.model.CancelSubscriptionCommand;
import com.tchalanet.server.core.subscription.application.command.model.CancelSubscriptionResult;
import com.tchalanet.server.core.subscription.application.event.TenantSubscriptionCanceledEvent;
import com.tchalanet.server.core.subscription.application.port.out.SubscriptionPersistencePort;
import com.tchalanet.server.core.subscription.application.port.out.SubscriptionReaderPort;
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

    // 4. Publish event after-commit
    AfterCommit.run(() -> {
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
