package com.tchalanet.server.core.session.internal.infra.event;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.core.outlet.api.command.lifecycle.CloseDayMode;
import com.tchalanet.server.core.outlet.api.event.OutletDayClosedEvent;
import com.tchalanet.server.core.session.api.command.CloseOutletOpenSalesSessionsCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;


@Component
@RequiredArgsConstructor
public class OutletDayClosedSessionListener {

    private final CommandBus commandBus;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
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
