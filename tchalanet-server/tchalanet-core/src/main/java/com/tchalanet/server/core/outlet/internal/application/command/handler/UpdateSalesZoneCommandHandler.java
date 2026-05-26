package com.tchalanet.server.core.outlet.internal.application.command.handler.zone;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.outlet.api.command.zone.UpdateSalesZoneCommand;
import com.tchalanet.server.core.outlet.internal.application.port.out.SalesZoneReaderPort;
import com.tchalanet.server.core.outlet.internal.application.port.out.SalesZoneWriterPort;
import com.tchalanet.server.core.outlet.internal.domain.model.SalesZone;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;

@UseCase
@RequiredArgsConstructor
public class UpdateSalesZoneCommandHandler implements VoidCommandHandler<UpdateSalesZoneCommand> {

    private final SalesZoneReaderPort reader;
    private final SalesZoneWriterPort writer;
    private final Clock clock;

    @Override
    @TchTx
    public void handle(UpdateSalesZoneCommand cmd) {
        SalesZone zone = reader.getRequired(cmd.tenantId(), cmd.zoneId());
        var now = Instant.now(clock);
        if (cmd.label() != null) {
            zone = zone.withLabel(cmd.label());
        }
        if (cmd.active() != null) {
            zone = zone.withActive(cmd.active());
        }
        zone = zone.withUpdatedAt(now);
        writer.save(zone);
    }
}
