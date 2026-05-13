package com.tchalanet.server.core.sales.api.event;

import com.tchalanet.server.core.draw.internal.domain.event.DrawResultAppliedEvent;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.core.sales.api.command.RecordDrawTicketsResultCommand;
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

        commandBus.execute(new RecordDrawTicketsResultCommand(
            event.drawId(),
            event.tenantId(),
            event.drawResultId(),
            event.occurredAt()));
    }
}
