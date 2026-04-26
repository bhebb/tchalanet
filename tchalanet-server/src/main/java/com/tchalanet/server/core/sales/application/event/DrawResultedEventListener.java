package com.tchalanet.server.core.sales.application.event;

import com.tchalanet.server.core.draw.domain.event.DrawResultAppliedEvent;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.core.sales.application.command.model.RecordDrawTicketsResultCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class DrawResultedEventListener {

    private final CommandBus commandBus;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDrawResultApplied(DrawResultAppliedEvent event) {
        log.info("DrawResultAppliedEvent received: tenantId={} drawId={} drawResultId={}",
            event.tenantId(), event.drawId(), event.drawResultId());

        commandBus.send(new RecordDrawTicketsResultCommand(
            event.tenantId(), event.drawId(), event.drawResultId(), event.occurredAt()));
    }
}
