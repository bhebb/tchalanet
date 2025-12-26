package com.tchalanet.server.core.billing.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.error.ProblemRestException;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.billing.application.command.model.ChangePlanCommand;
import com.tchalanet.server.core.billing.application.port.out.*;
import com.tchalanet.server.core.billing.domain.model.*;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ChangePlanCommandHandler implements CommandHandler<ChangePlanCommand, Subscription> {

  private final PlanReaderPort planReader;
  private final SubscriptionReaderPort subscriptionReader;
  private final SubscriptionWriterPort subscriptionWriter;
  private final BillingProviderPort billingProvider;

  @Override
  @TchTx
  public Subscription handle(ChangePlanCommand command) {

    Plan targetPlan =
        planReader
            .findById(command.planId())
            .filter(Plan::publicPlan)
            .orElseThrow(() -> ProblemRestException.notFound("Plan not found or not public"));

    Subscription current =
        subscriptionReader
            .findFirstByTenantIdAndStatusInOrderByCurrentPeriodStartDesc(
                command.tenantId(), List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.TRIALING))
            .orElseGet(
                () ->
                    Subscription.start(
                        command.subscriptionId(),
                        command.tenantId(),
                        targetPlan.id(),
                        Instant.now(),
                        BillingProvider.NONE));

    Subscription updated = current.changePlan(targetPlan, Instant.now());
    Subscription saved = subscriptionWriter.save(updated);

    billingProvider.changePlan(
        new BillingParams(command.tenantId(), saved.id()),
        targetPlan.code() // idéalement targetPlan.externalKey()
        );

    return saved;
  }
}
