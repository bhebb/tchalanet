package com.tchalanet.server.core.outlet.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.outlet.api.command.ReopenOutletDayCommand;
import com.tchalanet.server.core.outlet.api.command.ReopenOutletDayResult;
import com.tchalanet.server.core.outlet.internal.application.port.out.OutletReaderPort;
import com.tchalanet.server.core.outlet.internal.application.port.out.OutletWriterPort;
import com.tchalanet.server.core.outlet.internal.domain.event.OutletDayReopenedEvent;
import com.tchalanet.server.core.outlet.internal.domain.model.Outlet;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ReopenOutletDayCommandHandler
    implements CommandHandler<ReopenOutletDayCommand, ReopenOutletDayResult> {

  private final OutletReaderPort reader;
  private final OutletWriterPort writer;
  private final DomainEventPublisher publisher;
  private final IdGenerator idGenerator;
  private final Clock clock;

  @Override
  @TchTx
  public ReopenOutletDayResult handle(ReopenOutletDayCommand cmd) {
    var outlet = reader.getRequired(cmd.outletId());
    boolean wasClosed = outlet.dayClosed();
    var updated = outlet.reopenDay();
    writer.save(updated);

    if (wasClosed) {
      var event =
          new OutletDayReopenedEvent(
              EventId.of(idGenerator.newUuid()),
              Instant.now(clock),
              cmd.tenantId(),
              cmd.outletId(),
              cmd.date());
      AfterCommit.run(() -> publisher.publish(event));
    }
    return new ReopenOutletDayResult(
        updated.id(), wasClosed, updated.salesBlocked(), updated.salesBlockReason());
  }
}
