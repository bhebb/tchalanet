package com.tchalanet.server.core.billing.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.error.ProblemRestException;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.billing.application.command.model.ResumeSubscriptionCommand;
import com.tchalanet.server.core.billing.application.port.out.BillingParams;
import com.tchalanet.server.core.billing.application.port.out.BillingProviderPort;
import com.tchalanet.server.core.billing.application.port.out.SubscriptionReaderPort;
import com.tchalanet.server.core.billing.application.port.out.SubscriptionWriterPort;
import com.tchalanet.server.core.billing.domain.model.Subscription;
import com.tchalanet.server.core.billing.domain.model.SubscriptionStatus;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.List;

@UseCase
@RequiredArgsConstructor
public class ResumeSubscriptionCommandHandler
    implements CommandHandler<ResumeSubscriptionCommand, Subscription> {

    private final SubscriptionReaderPort subscriptionReader;
    private final SubscriptionWriterPort subscriptionWriter;
    private final BillingProviderPort billingProvider;

    @Override
    @TchTx
    public Subscription handle(ResumeSubscriptionCommand command) {
        var subscription =
            subscriptionReader
                .findFirstByTenantIdAndStatusInOrderByCurrentPeriodStartDesc(
                    command.tenantId(), List.of(SubscriptionStatus.CANCELED))
                .orElseThrow(() -> ProblemRestException.notFound("No canceled subscription to resume"));

        // Domaine doit porter la règle (idéal), mais à défaut:
        var resumed = subscription.resume(Instant.now());
        var saved = subscriptionWriter.save(resumed);

        billingProvider.resume(new BillingParams(
            command.tenantId(),
            saved.id()
        ));

        return saved;
    }
}
