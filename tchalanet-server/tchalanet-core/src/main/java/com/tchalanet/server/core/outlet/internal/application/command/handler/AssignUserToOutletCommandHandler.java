package com.tchalanet.server.core.outlet.internal.application.command.handler.assignment;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.outlet.api.command.assignment.AssignUserToOutletCommand;
import com.tchalanet.server.core.outlet.internal.application.port.out.OutletMembershipPort;
import com.tchalanet.server.core.outlet.internal.application.port.out.OutletReaderPort;
import com.tchalanet.server.core.outlet.internal.domain.event.OutletUserAssignedEvent;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;

@UseCase
@RequiredArgsConstructor
public class AssignUserToOutletCommandHandler
    implements VoidCommandHandler<AssignUserToOutletCommand> {

    private final OutletReaderPort outletReader;
    private final OutletMembershipPort membershipPort;
    private final DomainEventPublisher publisher;
    private final IdGenerator idGenerator;
    private final Clock clock;

    @Override
    @TchTx
    public void handle(AssignUserToOutletCommand cmd) {
        // Existence check (throws if outlet missing)
        outletReader.getRequired(cmd.outletId());

        membershipPort.assignUserToOutlet(cmd.outletId(), cmd.userId());

        Instant when = Instant.now(clock);
        OutletUserAssignedEvent event =
            new OutletUserAssignedEvent(
                EventId.of(idGenerator.newUuid()),
                when,
                cmd.tenantId(),
                cmd.outletId(),
                cmd.userId(),
                cmd.actorUserId());
        AfterCommit.run(() -> publisher.publish(event));
    }
}
