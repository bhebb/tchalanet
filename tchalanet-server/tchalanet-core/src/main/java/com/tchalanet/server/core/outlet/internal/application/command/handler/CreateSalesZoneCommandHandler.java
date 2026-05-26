package com.tchalanet.server.core.outlet.internal.application.command.handler.zone;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.SalesZoneId;
import com.tchalanet.server.core.outlet.api.command.zone.CreateSalesZoneCommand;
import com.tchalanet.server.core.outlet.internal.application.port.out.SalesZoneWriterPort;
import com.tchalanet.server.core.outlet.internal.domain.model.SalesZone;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;

@UseCase
@RequiredArgsConstructor
public class CreateSalesZoneCommandHandler
    implements CommandHandler<CreateSalesZoneCommand, SalesZoneId> {

    private final SalesZoneWriterPort writer;
    private final IdGenerator idGenerator;
    private final Clock clock;

    @Override
    @TchTx
    public SalesZoneId handle(CreateSalesZoneCommand cmd) {
        var newId = SalesZoneId.of(idGenerator.newUuid());
        var now = Instant.now(clock);
        var zone = SalesZone.createNew(
            newId, cmd.tenantId(), cmd.code(), cmd.label(), cmd.parentId(), now);
        var saved = writer.save(zone);
        return saved.id();
    }
}
