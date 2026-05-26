package com.tchalanet.server.core.seller.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.SellerOutletAssignmentId;
import com.tchalanet.server.core.seller.api.command.AssignSellerToOutletCommand;
import com.tchalanet.server.core.seller.api.model.SellerAssignmentStatus;
import com.tchalanet.server.core.seller.internal.application.port.out.SellerReaderPort;
import com.tchalanet.server.core.seller.internal.application.port.out.SellerWriterPort;
import com.tchalanet.server.core.seller.internal.domain.event.SellerAssignedToOutletEvent;
import com.tchalanet.server.core.seller.internal.domain.event.SellerAssignmentEndedEvent;
import com.tchalanet.server.core.seller.internal.domain.model.SellerOutletAssignment;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;

@UseCase
@RequiredArgsConstructor
public class AssignSellerToOutletCommandHandler implements CommandHandler<AssignSellerToOutletCommand, SellerOutletAssignmentId> {

    private final SellerReaderPort reader;
    private final SellerWriterPort writer;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final DomainEventPublisher publisher;

    @Override
    @TchTx
    public SellerOutletAssignmentId handle(AssignSellerToOutletCommand command) {
        var now = Instant.now(clock);

        // Close existing active assignment if any
        reader.findActiveAssignment(command.tenantId(), command.sellerId())
            .ifPresent(existing -> {
                var ended = existing.end(now);
                writer.saveAssignment(ended);
                AfterCommit.run(() -> publisher.publish(new SellerAssignmentEndedEvent(
                    EventId.of(idGenerator.newUuid()), now, command.tenantId(),
                    command.sellerId(), existing.id())));
            });

        var assignmentId = SellerOutletAssignmentId.of(idGenerator.newUuid());
        var assignment = new SellerOutletAssignment(
            assignmentId,
            command.tenantId(),
            command.sellerId(),
            command.outletId(),
            command.startsAt(),
            null,
            SellerAssignmentStatus.ACTIVE,
            now,
            now
        );

        var saved = writer.saveAssignment(assignment);

        AfterCommit.run(() -> publisher.publish(new SellerAssignedToOutletEvent(
            EventId.of(idGenerator.newUuid()), now, command.tenantId(),
            command.sellerId(), command.outletId(), saved.id())));

        return saved.id();
    }
}
