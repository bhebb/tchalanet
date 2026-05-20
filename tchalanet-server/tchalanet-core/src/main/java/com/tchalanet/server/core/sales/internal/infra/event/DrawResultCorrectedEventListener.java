package com.tchalanet.server.core.sales.internal.infra.event;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.core.draw.api.event.DrawResultCorrectedEvent;
import com.tchalanet.server.core.sales.api.command.result.ReconcileTicketsForCorrectedDrawResultCommand;
import com.tchalanet.server.platform.idempotence.api.ProcessedEventPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Profile("never")
@RequiredArgsConstructor
@Slf4j
public class DrawResultCorrectedEventListener {

    private static final String HANDLER_KEY = "sales.draw-result-corrected.reconcile";

    private final ProcessedEventPort processedEventPort;
    private final CommandBus commandBus;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDrawResultCorrected(DrawResultCorrectedEvent event) {
        if (!processedEventPort.markProcessedIfAbsent(HANDLER_KEY, event.eventId().value())) {
            log.debug("draw.result.corrected already processed eventId={}", event.eventId().value());
            return;
        }

        commandBus.execute(new ReconcileTicketsForCorrectedDrawResultCommand(
            event.tenantId(),
            event.drawId(),
            event.previousDrawResultId(),
            event.correctedDrawResultId(),
            event.reason()
        ));
    }
}
