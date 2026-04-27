package com.tchalanet.server.core.limitpolicy.application.command.handler;

import com.tchalanet.server.common.idempotency.event.ProcessedEventPort;
import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.limitpolicy.application.command.model.ApplyTicketExposureCommand;
import com.tchalanet.server.core.limitpolicy.application.port.out.ExposureProjectorPort;
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
        if (processedEvent.alreadyProcessed(HANDLER_KEY, eventId)) {
            return;
        }

        projector.applyTicketPlaced(e);

        processedEvent.markProcessed(HANDLER_KEY, eventId);
    }
}
