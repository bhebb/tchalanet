package com.tchalanet.server.core.limitpolicy.internal.application.command.handler.exposure;

import com.tchalanet.server.platform.idempotence.api.ProcessedEventPort;
import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.limitpolicy.api.command.ApplyTicketExposureCommand;
import com.tchalanet.server.core.limitpolicy.internal.application.port.out.exposure.ExposureProjectorPort;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class ApplyTicketExposureCommandHandler implements VoidCommandHandler<ApplyTicketExposureCommand> {

    private static final String HANDLER_KEY = "limitpolicy.exposure";

    private final ProcessedEventPort processedEvent;
    private final ExposureProjectorPort projector;

    @Override
    @TchTx
    public void handle(ApplyTicketExposureCommand c) {
        var e = c.event();

        UUID eventId = e.eventId().value();
        if (!processedEvent.markProcessedIfAbsent(HANDLER_KEY, eventId)) {
            return;
        }

        projector.applyTicketSold(e);
    }
}
