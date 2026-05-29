package com.tchalanet.server.core.seller.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.SellerCommissionPolicyId;
import com.tchalanet.server.core.seller.api.command.SetSellerCommissionPolicyCommand;
import com.tchalanet.server.core.seller.api.model.SellerAssignmentStatus;
import com.tchalanet.server.core.seller.internal.application.port.out.SellerReaderPort;
import com.tchalanet.server.core.seller.internal.application.port.out.SellerWriterPort;
import com.tchalanet.server.core.seller.internal.domain.event.SellerCommissionPolicyChangedEvent;
import com.tchalanet.server.core.seller.internal.domain.model.SellerCommissionPolicy;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;

@UseCase
@RequiredArgsConstructor
public class SetSellerCommissionPolicyCommandHandler implements CommandHandler<SetSellerCommissionPolicyCommand, SellerCommissionPolicyId> {

    private final SellerReaderPort reader;
    private final SellerWriterPort writer;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final DomainEventPublisher publisher;

    @Override
    @TchTx
    public SellerCommissionPolicyId handle(SetSellerCommissionPolicyCommand command) {
        var now = Instant.now(clock);

        // Close existing active policy
        reader.findActiveCommissionPolicy(command.tenantId(), command.sellerId())
            .ifPresent(existing -> writer.saveCommissionPolicy(existing.end(now)));

        var policyId = SellerCommissionPolicyId.of(idGenerator.newUuid());
        var policy = new SellerCommissionPolicy(
            policyId,
            command.tenantId(),
            command.sellerId(),
            command.type(),
            command.base(),
            command.ratePercent(),
            command.fixedAmount(),
            command.currency(),
            command.startsAt(),
            null,
            SellerAssignmentStatus.ACTIVE,
            now,
            now
        );

        var saved = writer.saveCommissionPolicy(policy);

        AfterCommit.run(() -> publisher.publish(new SellerCommissionPolicyChangedEvent(
            EventId.of(idGenerator.newUuid()), now, command.tenantId(),
            command.sellerId(), saved.id(), saved.type())));

        return saved.id();
    }
}
