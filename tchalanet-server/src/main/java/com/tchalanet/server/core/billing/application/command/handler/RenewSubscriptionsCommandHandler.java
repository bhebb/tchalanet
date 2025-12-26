package com.tchalanet.server.core.billing.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.billing.application.command.model.RenewSubscriptionsCommand;
import com.tchalanet.server.core.billing.application.port.out.BillingParams;
import com.tchalanet.server.core.billing.application.port.out.BillingProviderPort;
import com.tchalanet.server.core.billing.application.port.out.SubscriptionReaderPort;
import com.tchalanet.server.core.billing.application.port.out.SubscriptionWriterPort;
import com.tchalanet.server.core.billing.domain.model.Subscription;
import com.tchalanet.server.core.billing.domain.model.SubscriptionStatus;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class RenewSubscriptionsCommandHandler
    implements VoidCommandHandler<RenewSubscriptionsCommand> {

  private final SubscriptionReaderPort subscriptionReader;
  private final SubscriptionWriterPort subscriptionWriter;
  private final BillingProviderPort billingProvider; // optionnel, mais utile (LOG_ONLY)

  @Override
  @TchTx
  public void handle(RenewSubscriptionsCommand command) {
    var now = Instant.now();

    List<Subscription> due =
        subscriptionReader.findByStatusAndCurrentPeriodEndBefore(SubscriptionStatus.ACTIVE, now);

    for (Subscription sub : due) {
      try {
        // V1: règle simple. Idéalement -> sub.renew(now) calcule nextEnd selon plan/frequency.
        var newEnd = sub.currentPeriodEnd().plus(30, ChronoUnit.DAYS);

        var renewed = sub.renew(newEnd); // si tu peux ajouter now, sinon renew(newEnd)
        var saved = subscriptionWriter.save(renewed);

        log.info(
            "Renewed subscription tenantId={} subscriptionId={} newEnd={}",
            saved.tenantId(),
            saved.id(),
            newEnd);

        billingProvider.changePlan(new BillingParams(saved.tenantId(), saved.id()), "NOOP_RENEW");
      } catch (Exception e) {
        log.error(
            "Failed to renew subscription tenantId={} subscriptionId={}",
            sub.tenantId(),
            sub.id(),
            e);
      }
    }
  }
}
