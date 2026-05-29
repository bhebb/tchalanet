package com.tchalanet.server.core.seller.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.seller.api.command.EndSellerAssignmentCommand;
import com.tchalanet.server.core.seller.internal.application.port.out.SellerReaderPort;
import com.tchalanet.server.core.seller.internal.application.port.out.SellerWriterPort;
import com.tchalanet.server.core.seller.internal.domain.event.SellerAssignmentEndedEvent;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;

@UseCase
@RequiredArgsConstructor
public class EndSellerAssignmentCommandHandler implements CommandHandler<EndSellerAssignmentCommand, Void> {

    private final SellerReaderPort reader;
    private final SellerWriterPort writer;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final DomainEventPublisher publisher;

    @Override
    @TchTx
    public Void handle(EndSellerAssignmentCommand command) {
        var now = Instant.now(clock);
        var assignment = reader.findAssignment(command.tenantId(), command.assignmentId())
            .orElseThrow(() -> ProblemRest.notFound("seller.assignment_not_found"));

        if (!assignment.sellerId().equals(command.sellerId())) {
            throw ProblemRest.badRequest("seller.assignment_seller_mismatch");
        }

        var ended = assignment.end(now);
        writer.saveAssignment(ended);

        AfterCommit.run(() -> publisher.publish(new SellerAssignmentEndedEvent(
            EventId.of(idGenerator.newUuid()), now, command.tenantId(),
            command.sellerId(), command.assignmentId())));

        return null;
    }
}
