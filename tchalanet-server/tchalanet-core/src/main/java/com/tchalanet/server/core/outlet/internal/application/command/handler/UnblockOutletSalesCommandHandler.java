package com.tchalanet.server.core.outlet.internal.application.command.handler.block;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.outlet.api.command.block.UnblockOutletSalesCommand;
import com.tchalanet.server.core.outlet.internal.application.port.out.OutletReaderPort;
import com.tchalanet.server.core.outlet.internal.application.port.out.OutletWriterPort;
import com.tchalanet.server.core.outlet.internal.domain.event.OutletSalesUnblockedEvent;
import com.tchalanet.server.core.outlet.internal.domain.model.Outlet;

import java.time.Clock;
import java.time.Instant;

import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UnblockOutletSalesCommandHandler
    implements VoidCommandHandler<UnblockOutletSalesCommand> {

    private final OutletReaderPort reader;
    private final OutletWriterPort writer;
    private final DomainEventPublisher publisher;
    private final IdGenerator idGenerator;
    private final Clock clock;

    @Override
    @TchTx
    public void handle(UnblockOutletSalesCommand cmd) {
        var outlet = reader.getRequired(cmd.outletId());
        var updated = outlet.unblockSales();
        if (updated == outlet) return; // idempotent: already unblocked

        writer.save(updated);
        var when = Instant.now(clock);
        var event =
            new OutletSalesUnblockedEvent(
                EventId.of(idGenerator.newUuid()),
                when,
                cmd.tenantId(),
                cmd.outletId(),
                cmd.actorUserId());
        AfterCommit.run(() -> publisher.publish(event));
    }
}
