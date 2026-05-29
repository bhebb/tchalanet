package com.tchalanet.server.core.outlet.internal.application.command.handler.block;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.outlet.api.command.block.BlockOutletSalesCommand;
import com.tchalanet.server.core.outlet.internal.application.port.out.OutletReaderPort;
import com.tchalanet.server.core.outlet.internal.application.port.out.OutletWriterPort;
import com.tchalanet.server.core.outlet.internal.domain.event.OutletSalesBlockedEvent;
import com.tchalanet.server.core.outlet.internal.domain.model.Outlet;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;

@UseCase
@RequiredArgsConstructor
public class BlockOutletSalesCommandHandler
    implements VoidCommandHandler<BlockOutletSalesCommand> {

    private final OutletReaderPort reader;
    private final OutletWriterPort writer;
    private final DomainEventPublisher publisher;
    private final IdGenerator idGenerator;
    private final Clock clock;

    @Override
    @TchTx
    public void handle(BlockOutletSalesCommand cmd) {
        Outlet outlet = reader.getRequired(cmd.outletId());
        Instant when = Instant.now(clock);
        Outlet updated = outlet.blockSales(cmd.reason(), when, cmd.actorUserId());
        if (updated.equals(outlet)) return; // idempotent: already blocked with same state

        writer.save(updated);
        OutletSalesBlockedEvent event =
            new OutletSalesBlockedEvent(
                EventId.of(idGenerator.newUuid()),
                when,
                cmd.tenantId(),
                cmd.outletId(),
                cmd.reason(),
                cmd.actorUserId());
        AfterCommit.run(() -> publisher.publish(event));
    }
}
