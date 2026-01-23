package com.tchalanet.server.catalog.billing.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.error.ProblemRestException;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.catalog.billing.application.command.model.CancelSubscriptionCommand;
import com.tchalanet.server.catalog.billing.application.port.out.BillingParams;
import com.tchalanet.server.catalog.billing.application.port.out.BillingProviderPort;
import com.tchalanet.server.catalog.billing.application.port.out.SubscriptionReaderPort;
import com.tchalanet.server.catalog.billing.application.port.out.SubscriptionWriterPort;
import com.tchalanet.server.catalog.billing.domain.model.Subscription;
import com.tchalanet.server.catalog.billing.domain.model.SubscriptionStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CancelSubscriptionCommandHandler
    implements CommandHandler<CancelSubscriptionCommand, Subscription> {

  private static final Set<SubscriptionStatus> ACTIVE_STATUSES =
      Set.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.TRIALING);

  private final SubscriptionReaderPort subscriptionReader;
  private final SubscriptionWriterPort subscriptionWriter;
  private final BillingProviderPort billingProvider;
  private final Clock clock;

  @Override
  @TchTx
  public Subscription handle(CancelSubscriptionCommand command) {
    var subscription =
        subscriptionReader
            .findFirstByTenantIdAndStatus(command.tenantId(), ACTIVE_STATUSES)
            .orElseThrow(() -> ProblemRestException.notFound("No active subscription"));

    Subscription updated;
    var now = Instant.now(clock);
    if (command.atPeriodEnd()) {
      updated = subscription.scheduleCancellation(now);
      var saved = subscriptionWriter.save(updated);
      billingProvider.cancelAtPeriodEnd(new BillingParams(command.tenantId(), saved.id()));
      return saved;
    } else {
      updated = subscription.cancelNow(now);
      var saved = subscriptionWriter.save(updated);
      billingProvider.cancelImmediately(new BillingParams(command.tenantId(), saved.id()));
      return saved;
    }
  }
}
