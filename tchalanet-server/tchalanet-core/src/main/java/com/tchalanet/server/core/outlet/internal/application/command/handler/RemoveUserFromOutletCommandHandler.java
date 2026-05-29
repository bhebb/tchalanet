package com.tchalanet.server.core.outlet.internal.application.command.handler.assignment;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.outlet.api.command.assignment.RemoveUserFromOutletCommand;
import com.tchalanet.server.core.outlet.internal.application.port.out.OutletMembershipPort;
import com.tchalanet.server.core.outlet.internal.application.port.out.OutletReaderPort;
import com.tchalanet.server.core.outlet.internal.domain.event.OutletUserRemovedEvent;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class RemoveUserFromOutletCommandHandler
    implements VoidCommandHandler<RemoveUserFromOutletCommand> {

  private final OutletReaderPort outletReader;
  private final OutletMembershipPort membershipPort;
  private final DomainEventPublisher publisher;
  private final IdGenerator idGenerator;
  private final Clock clock;

  @Override
  @TchTx
  public void handle(RemoveUserFromOutletCommand cmd) {
    outletReader.getRequired(cmd.outletId());

    membershipPort.removeUserFromOutlet(cmd.outletId(), cmd.userId());

    var when = Instant.now(clock);
    var event =
        new OutletUserRemovedEvent(
            EventId.of(idGenerator.newUuid()),
            when,
            cmd.tenantId(),
            cmd.outletId(),
            cmd.userId(),
            cmd.actorUserId());
    AfterCommit.run(() -> publisher.publish(event));
  }
}
