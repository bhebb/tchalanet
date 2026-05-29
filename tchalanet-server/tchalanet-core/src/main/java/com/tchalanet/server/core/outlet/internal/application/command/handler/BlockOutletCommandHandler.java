package com.tchalanet.server.core.outlet.internal.application.command.handler.block;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.outlet.api.command.block.BlockOutletCommand;
import com.tchalanet.server.core.outlet.internal.application.port.out.OutletReaderPort;
import com.tchalanet.server.core.outlet.internal.application.port.out.OutletWriterPort;
import com.tchalanet.server.core.outlet.internal.domain.event.OutletBlockedEvent;
import com.tchalanet.server.core.outlet.internal.domain.model.Outlet;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;

@UseCase
@RequiredArgsConstructor
public class BlockOutletCommandHandler implements VoidCommandHandler<BlockOutletCommand> {

    private final OutletReaderPort reader;
    private final OutletWriterPort writer;
    private final DomainEventPublisher publisher;
    private final IdGenerator idGenerator;
    private final Clock clock;

    @Override
    @TchTx
    public void handle(BlockOutletCommand cmd) {
        Outlet outlet = reader.getRequired(cmd.outletId());
        if (outlet.outletBlock().blocked()) return; // idempotent: already blocked

        Instant when = Instant.now(clock);
        Outlet updated = outlet.blockOutlet(cmd.reason(), when, null);

        writer.save(updated);

        OutletBlockedEvent event = new OutletBlockedEvent(
            EventId.of(idGenerator.newUuid()),
            when,
            cmd.tenantId(),
            cmd.outletId(),
            cmd.reason());
        AfterCommit.run(() -> publisher.publish(event));
    }
}
