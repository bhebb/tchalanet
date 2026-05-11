package com.tchalanet.server.core.session.infra.event;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.core.outlet.application.command.model.CloseDayMode;
import com.tchalanet.server.core.outlet.domain.event.OutletDayClosedEvent;
import com.tchalanet.server.core.session.application.command.model.CloseOutletOpenSalesSessionsCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class OutletDayClosedSessionListener {

    private final CommandBus commandBus;

    @EventListener
    public void onOutletDayClosed(OutletDayClosedEvent event) {
        if (event.mode() == CloseDayMode.STRICT) {
            return;
        }

        commandBus.execute(
            new CloseOutletOpenSalesSessionsCommand(
                event.tenantId(),
                event.outletId(),
                event.closedDate(),
                event.occurredAt(),
                event.actorUserId(),
                "Closed by outlet day"));
    }
}
