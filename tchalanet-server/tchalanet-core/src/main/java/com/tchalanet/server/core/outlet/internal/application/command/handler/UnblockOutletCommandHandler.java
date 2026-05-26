package com.tchalanet.server.core.outlet.internal.application.command.handler.block;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.outlet.api.command.block.UnblockOutletCommand;
import com.tchalanet.server.core.outlet.internal.application.port.out.OutletReaderPort;
import com.tchalanet.server.core.outlet.internal.application.port.out.OutletWriterPort;
import com.tchalanet.server.core.outlet.internal.domain.event.OutletUnblockedEvent;
import com.tchalanet.server.core.outlet.internal.domain.model.Outlet;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;

@UseCase
@RequiredArgsConstructor
public class UnblockOutletCommandHandler implements VoidCommandHandler<UnblockOutletCommand> {

    private final OutletReaderPort reader;
    private final OutletWriterPort writer;
    private final DomainEventPublisher publisher;
    private final IdGenerator idGenerator;
    private final Clock clock;

    @Override
    @TchTx
    public void handle(UnblockOutletCommand cmd) {
        Outlet outlet = reader.getRequired(cmd.outletId());
        Outlet updated = outlet.unblockOutlet();
        if (updated == outlet) return; // idempotent: already unblocked

        writer.save(updated);

        Instant when = Instant.now(clock);
        OutletUnblockedEvent event = new OutletUnblockedEvent(
            EventId.of(idGenerator.newUuid()),
            when,
            cmd.tenantId(),
            cmd.outletId());
        AfterCommit.run(() -> publisher.publish(event));
    }
}
