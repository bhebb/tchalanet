package com.tchalanet.server.core.sales.internal.infra.event;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.platform.idempotence.api.ProcessedEventPort;
import com.tchalanet.server.core.draw.api.event.DrawResultAppliedEvent;
import com.tchalanet.server.core.draw.api.event.DrawResultCorrectedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class DrawResultAppliedSalesEventListener {

    private static final String APPLIED_HANDLER_KEY = "sales.draw-result-applied";
    private static final String CORRECTED_HANDLER_KEY = "sales.draw-result-corrected";

    private final CommandBus commandBus;
    private final ProcessedEventPort processedEventPort;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDrawResultApplied(DrawResultAppliedEvent event) {
        if (processedEventPort.alreadyProcessed(APPLIED_HANDLER_KEY, event.eventId().value())) {
            log.info(
                "Duplicate DrawResultAppliedEvent ignored eventId={} tenantId={} drawId={} drawResultId={}",
                event.eventId(), event.tenantId(), event.drawId(), event.drawResultId()
            );
            return;
        }

        log.info(
            "DrawResultAppliedEvent received eventId={} tenantId={} drawId={} drawResultId={}",
            event.eventId(), event.tenantId(), event.drawId(), event.drawResultId()
        );

        commandBus.execute(new com.tchalanet.server.core.sales.api.command.result.RecordDrawTicketsResultCommand(
            event.tenantId(),
            event.drawId(),
            event.drawResultId(),
            event.drawDate(),
            event.resultSlotId(),
            event.drawChannelId()
        ));

        processedEventPort.markProcessed(APPLIED_HANDLER_KEY, event.eventId().value());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDrawResultCorrected(DrawResultCorrectedEvent event) {
        if (processedEventPort.alreadyProcessed(CORRECTED_HANDLER_KEY, event.eventId().value())) {
            log.info(
                "Duplicate DrawResultCorrectedEvent ignored eventId={} tenantId={} drawId={} correctedDrawResultId={}",
                event.eventId(), event.tenantId(), event.drawId(), event.correctedDrawResultId()
            );
            return;
        }

        log.info(
            "DrawResultCorrectedEvent received eventId={} tenantId={} drawId={} previousDrawResultId={} correctedDrawResultId={}",
            event.eventId(),
            event.tenantId(),
            event.drawId(),
            event.previousDrawResultId(),
            event.correctedDrawResultId()
        );

        commandBus.execute(new com.tchalanet.server.core.sales.api.command.result.ReconcileTicketsForCorrectedDrawResultCommand(
            event.tenantId(),
            event.drawId(),
            event.previousDrawResultId(),
            event.correctedDrawResultId(),
            event.reason()
        ));

        processedEventPort.markProcessed(CORRECTED_HANDLER_KEY, event.eventId().value());
    }
}
